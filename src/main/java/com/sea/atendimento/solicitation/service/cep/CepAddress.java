package com.sea.atendimento.solicitation.service.cep;

import com.sea.atendimento.shared.domain.Uf;

public record CepAddress(String cep, String street, String neighborhood, String city, Uf uf) { }