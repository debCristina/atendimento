package com.sea.atendimento.solicitation.service;

import java.util.Optional;

public interface CepLookup {

    /**
     * Busca o endereço de um CEP.
     *
     * @return o endereço, ou vazio se o CEP não existir
     * @throws com.sea.atendimento.shared.exception.ExternalServiceException
     *         se o serviço de CEP falhar ou não responder
     */
    Optional<CepAddress> findByCep(String cep);
}