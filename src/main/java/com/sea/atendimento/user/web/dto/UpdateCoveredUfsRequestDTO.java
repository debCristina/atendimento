package com.sea.atendimento.user.web.dto;

import com.sea.atendimento.shared.domain.Uf;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateCoveredUfsRequestDTO(

        @NotEmpty(message = "Informe pelo menos uma UF")
        Set<Uf> coveredUfs
) { }