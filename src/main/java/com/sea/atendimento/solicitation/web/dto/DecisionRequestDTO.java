package com.sea.atendimento.solicitation.web.dto;

import com.sea.atendimento.solicitation.domain.Decision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.sea.atendimento.shared.utils.Strings.trimToNull;

public record DecisionRequestDTO(
    @NotNull(message = "A decisão é obrigatória") 
    Decision decision,

    @NotBlank(message = "O comentário é obrigatório") 
    @Size(min = 10, max = 1000, message = "O comentário deve ter entre 10 e 1000 caracteres") 
    String comment

) {

    public DecisionRequestDTO {
        comment = trimToNull(comment);
    }
}
