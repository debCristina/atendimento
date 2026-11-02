package com.sea.atendimento.solicitation.web.dto;

import com.sea.atendimento.solicitation.domain.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.sea.atendimento.shared.utils.Strings.trimToNull;

public record BasicDataRequestDTO(

        @NotNull(message = "O tipo de serviço é obrigatório")
        ServiceType serviceType,

        @NotBlank(message = "O título é obrigatório")
        @Size(min = 3, max = 80, message = "O título deve ter entre 3 e 80 caracteres")
        String title,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(min = 20, max = 1000, message = "A descrição deve ter entre 20 e 1000 caracteres")
        String description
) {
    public BasicDataRequestDTO {
        title = trimToNull(title);
        description = trimToNull(description);
    }
}
