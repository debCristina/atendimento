package com.sea.atendimento.search.infra;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SolicitationSearchRepository extends ElasticsearchRepository<SolicitationDocument, Long> {
}
