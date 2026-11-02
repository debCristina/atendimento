package com.sea.atendimento.solicitation.web.dto;

import com.sea.atendimento.solicitation.domain.Priority;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfirmationRequestDTO(

        @NotNull(message = "A prioridade é obrigatória")
        Priority priority,

        @NotNull(message = "A data preferencial é obrigatória")
        LocalDate preferredDate,

        @NotNull(message = "O valor estimado é obrigatório")
        @Digits(integer = 10, fraction = 2, message = "O valor estimado deve ter no máximo 2 casas decimais")
        BigDecimal estimatedValue,

        @NotNull(message = "O aceite dos termos é obrigatório")
        Boolean termsAccepted
) { }
