package com.sea.atendimento.search.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.sea.atendimento.search.infra.SolicitationDocument;
import com.sea.atendimento.search.web.dto.SolicitationSearchResponseDTO;
import com.sea.atendimento.search.web.dto.SolicitationSearchRequestDTO;
import com.sea.atendimento.shared.domain.Role;
import com.sea.atendimento.shared.domain.Uf;
import com.sea.atendimento.shared.exception.BusinessRuleException;
import com.sea.atendimento.shared.exception.NotFoundException;
import com.sea.atendimento.shared.security.AuthenticatedUser;
import com.sea.atendimento.solicitation.domain.SolicitationStatus;
import com.sea.atendimento.user.domain.User;
import com.sea.atendimento.user.infra.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class SolicitationSearchService {

    private static final int DEFAULT_SIZE = 20;
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("createdAt", "submittedAt", "estimatedValue", "preferredDate");

    private final ElasticsearchOperations operations;
    private final UserRepository userRepository;

    public SolicitationSearchService(ElasticsearchOperations operations, UserRepository userRepository) {
        this.operations = operations;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SolicitationSearchResponseDTO search(SolicitationSearchRequestDTO filter, AuthenticatedUser authenticated) {
        User user = userRepository.findById(authenticated.id())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        int page = filter.page() == null ? 0 : filter.page();
        int size = filter.size() == null ? DEFAULT_SIZE : filter.size();

        Set<Uf> allowedUfs = resolveAllowedUfs(user, filter.state());
        if (allowedUfs != null && allowedUfs.isEmpty()) {
            return SolicitationSearchResponseDTO.empty(page, size);
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(buildQuery(filter, allowedUfs))
                .withPageable(PageRequest.of(page, size, parseSort(filter.sort())))
                .build();

        SearchHits<SolicitationDocument> hits = operations.search(query, SolicitationDocument.class);

        List<SolicitationSearchResponseDTO.Item> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(SolicitationSearchResponseDTO.Item::from)
                .toList();

        return new SolicitationSearchResponseDTO(items, page, size, hits.getTotalHits());
    }

    // null = sem restrição de UF (admin sem filtro); vazio = nenhuma UF permitida
    private Set<Uf> resolveAllowedUfs(User user, Uf requestedState) {
        if (user.getRole() == Role.ADMIN) {
            return requestedState == null ? null : Set.of(requestedState);
        }
        Set<Uf> covered = user.getCoveredUfs();
        if (requestedState == null) {
            return covered;
        }
        return covered.contains(requestedState) ? Set.of(requestedState) : Set.of();
    }

    private Query buildQuery(SolicitationSearchRequestDTO filter, Set<Uf> allowedUfs) {
        return Query.of(q -> q.bool(b -> {
            b.mustNot(mn -> mn.term(t -> t.field("status").value(SolicitationStatus.DRAFT.name())));

            if (filter.q() != null && !filter.q().isBlank()) {
                b.must(m -> m.multiMatch(mm -> mm.query(filter.q()).fields("title", "description")));
            }
            if (filter.status() != null) {
                b.filter(f -> f.term(t -> t.field("status").value(filter.status().name())));
            }
            if (filter.serviceType() != null) {
                b.filter(f -> f.term(t -> t.field("serviceType").value(filter.serviceType().name())));
            }
            if (filter.priority() != null) {
                b.filter(f -> f.term(t -> t.field("priority").value(filter.priority().name())));
            }
            if (allowedUfs != null) {
                List<FieldValue> ufs = allowedUfs.stream().map(uf -> FieldValue.of(uf.name())).toList();
                b.filter(f -> f.terms(t -> t.field("uf").terms(v -> v.value(ufs))));
            }
            if (filter.dateFrom() != null || filter.dateTo() != null) {
                b.filter(f -> f.range(r -> r.date(d -> {
                    d.field("createdAt").timeZone("America/Sao_Paulo");
                    if (filter.dateFrom() != null) {
                        d.gte(filter.dateFrom().toString());
                    }
                    if (filter.dateTo() != null) {
                        d.lt(filter.dateTo().plusDays(1).toString());
                    }
                    return d;
                })));
            }
            return b;
        }));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!SORTABLE_FIELDS.contains(field)) {
            throw new BusinessRuleException("Ordenação inválida. Campos permitidos: " + SORTABLE_FIELDS);
        }
        boolean ascending = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc");
        return Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, field);
    }
}