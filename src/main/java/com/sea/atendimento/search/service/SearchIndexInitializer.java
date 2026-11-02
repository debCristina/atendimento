package com.sea.atendimento.search.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SearchIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexInitializer.class);

    private final SolicitationIndexer indexer;

    public SearchIndexInitializer(SolicitationIndexer indexer) {
        this.indexer = indexer;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int total = indexer.reindexAll();
            log.info("Índice de busca sincronizado: {} solicitações", total);
        } catch (Exception e) {
            log.warn("Não foi possível sincronizar o índice de busca na inicialização", e);
        }
    }
}