package com.sea.atendimento.solicitation.infra.viacep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record ViaCepResponse(String cep, String logradouro, String bairro, String localidade, String uf) { }