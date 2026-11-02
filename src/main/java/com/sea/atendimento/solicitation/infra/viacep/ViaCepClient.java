package com.sea.atendimento.solicitation.infra.viacep;

import com.sea.atendimento.shared.exception.ExternalServiceException;
import com.sea.atendimento.solicitation.service.CepAddress;
import com.sea.atendimento.solicitation.service.CepLookup;
import com.sea.atendimento.user.domain.Uf;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

@Component
public class ViaCepClient implements CepLookup {

    private final RestClient restClient;

    public ViaCepClient(@Value("${viacep.base-url}") String baseUrl,
                        @Value("${viacep.timeout}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<CepAddress> findByCep(String cep) {
        try {
            ViaCepResponse response = restClient.get()
                    .uri("/{cep}/json/", cep)
                    .retrieve()
                    .body(ViaCepResponse.class);

            if (response == null || response.uf() == null) {
                return Optional.empty();
            }

            return Optional.of(new CepAddress(
                    cep,
                    response.logradouro(),
                    response.bairro(),
                    response.localidade(),
                    Uf.valueOf(response.uf())
            ));

        } catch (HttpClientErrorException.BadRequest e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ExternalServiceException("O serviço de consulta de CEP está indisponível no momento", e);
        }
    }
}