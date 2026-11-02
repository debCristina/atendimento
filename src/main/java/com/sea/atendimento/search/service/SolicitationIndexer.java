package com.sea.atendimento.search.service;

import com.sea.atendimento.search.infra.SolicitationDocument;
import com.sea.atendimento.search.infra.SolicitationSearchRepository;
import com.sea.atendimento.solicitation.infra.SolicitationRepository;
import com.sea.atendimento.solicitation.service.SolicitationChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class SolicitationIndexer {

    private static final Logger log = LoggerFactory.getLogger(SolicitationIndexer.class);

    private final SolicitationRepository solicitationRepository;
    private final SolicitationSearchRepository searchRepository;

    public SolicitationIndexer(SolicitationRepository solicitationRepository,
                               SolicitationSearchRepository searchRepository) {
        this.solicitationRepository = solicitationRepository;
        this.searchRepository = searchRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onSolicitationChanged(SolicitationChangedEvent event) {
        try {
            solicitationRepository.findById(event.solicitationId())
                    .map(SolicitationDocument::from)
                    .ifPresent(searchRepository::save);
        } catch (Exception e) {
            log.warn("Falha ao indexar a solicitação {}", event.solicitationId(), e);
        }
    }

    @Transactional(readOnly = true)
    public int reindexAll() {
        List<SolicitationDocument> documents = solicitationRepository.findAll().stream()
                .map(SolicitationDocument::from)
                .toList();
        searchRepository.saveAll(documents);
        return documents.size();
    }
}
