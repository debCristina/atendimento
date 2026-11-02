package com.sea.atendimento.solicitation.service;

import com.sea.atendimento.user.domain.Uf;

public record CepAddress(String cep, String street, String neighborhood, String city, Uf uf) { }