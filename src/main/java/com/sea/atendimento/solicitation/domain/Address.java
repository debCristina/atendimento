package com.sea.atendimento.solicitation.domain;

import com.sea.atendimento.shared.domain.Uf;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Column(length = 8)
    private String cep;

    private String street;

    private String neighborhood;

    private String city;

    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    private Uf uf;

    @Column(length = 20)
    private String number;

    private String complement;

    public Address(String cep, String street, String neighborhood, String city,
                   Uf uf, String number, String complement) {
        this.cep = normalizeCep(cep);
        this.street = street;
        this.neighborhood = neighborhood;
        this.city = city;
        this.uf = uf;
        this.number = number.trim();
        this.complement = (complement == null || complement.isBlank()) ? null : complement.trim();
    }

    public static String normalizeCep(String cep) {
        return cep == null ? null : cep.replaceAll("\\D", "");
    }
}