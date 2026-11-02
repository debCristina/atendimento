package com.sea.atendimento.solicitation.web.dto;

import com.sea.atendimento.solicitation.domain.*;
import com.sea.atendimento.user.domain.Uf;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SolicitationResponseDTO(
        Long id,
        Long clientId,
        SolicitationStatus status,
        int currentStep,
        ServiceType serviceType,
        String title,
        String description,
        AddressResponse address,
        Priority priority,
        LocalDate preferredDate,
        BigDecimal estimatedValue,
        boolean termsAccepted,
        Long analystId,
        String decisionComment,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant decidedAt
) {
    public record AddressResponse(String cep, String street, String neighborhood,
                                  String city, Uf uf, String number, String complement) {

        static AddressResponse from(Address address) {
            if (address == null) {
                return null;
            }
            return new AddressResponse(address.getCep(), address.getStreet(), address.getNeighborhood(),
                    address.getCity(), address.getUf(), address.getNumber(), address.getComplement());
        }
    }

    public static SolicitationResponseDTO from(Solicitation s) {
        return new SolicitationResponseDTO(
                s.getId(),
                s.getClient().getId(),
                s.getStatus(),
                s.getCurrentStep(),
                s.getServiceType(),
                s.getTitle(),
                s.getDescription(),
                AddressResponse.from(s.getAddress()),
                s.getPriority(),
                s.getPreferredDate(),
                s.getEstimatedValue(),
                s.isTermsAccepted(),
                s.getAnalyst() == null ? null : s.getAnalyst().getId(),
                s.getDecisionComment(),
                s.getCreatedAt(),
                s.getUpdatedAt(),
                s.getSubmittedAt(),
                s.getDecidedAt()
        );
    }
}