package com.sea.atendimento.search.web.dto;

import com.sea.atendimento.shared.domain.Uf;
import com.sea.atendimento.solicitation.domain.Priority;
import com.sea.atendimento.solicitation.domain.ServiceType;
import com.sea.atendimento.solicitation.domain.SolicitationStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record SolicitationSearchRequestDTO(
        String q,
        SolicitationStatus status,
        ServiceType serviceType,
        Priority priority,
        Uf state,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateTo,

        @Min(value = 0, message = "A página deve ser maior ou igual a 0")
        Integer page,

        @Min(value = 1, message = "O tamanho da página deve ser de pelo menos 1")
        @Max(value = 100, message = "O tamanho da página deve ser de no máximo 100")
        Integer size,

        String sort
){}

