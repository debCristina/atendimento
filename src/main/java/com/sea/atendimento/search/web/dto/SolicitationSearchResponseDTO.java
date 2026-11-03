package com.sea.atendimento.search.web.dto;

import com.sea.atendimento.search.infra.SolicitationDocument;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SolicitationSearchResponseDTO(List<Item> items, int page, int size, long total) {

    public record Item(Long id, String title, String status, String serviceType, String priority,
                       String uf, String city, BigDecimal estimatedValue, LocalDate preferredDate,
                       Instant createdAt, Instant submittedAt) {

        public static Item from(SolicitationDocument d) {
            return new Item(d.id(), d.title(), d.status(), d.serviceType(), d.priority(),
                    d.uf(), d.city(), d.estimatedValue(), d.preferredDate(),
                    d.createdAt(), d.submittedAt());
        }
    }

    public static SolicitationSearchResponseDTO empty(int page, int size) {
        return new SolicitationSearchResponseDTO(List.of(), page, size, 0);
    }
}