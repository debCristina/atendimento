package com.sea.atendimento.solicitation.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.sea.atendimento.shared.utils.Strings.trimToNull;

public record AddressRequestDTO(

        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "O CEP deve ter 8 dígitos")
        String cep,

        @NotBlank(message = "O número é obrigatório")
        @Size(max = 20, message = "O número deve ter entre 1 e 20 caracteres")
        String number,

        @Size(max = 255, message = "O complemento deve ter no máximo 255 caracteres")
        String complement
) {
    public AddressRequestDTO {
        cep = trimToNull(cep);
        number = trimToNull(number);
        complement = trimToNull(complement);
    }
}