package com.sea.atendimento.solicitation.domain;

import com.sea.atendimento.shared.exception.BusinessRuleException;
import com.sea.atendimento.shared.exception.ConflictException;
import com.sea.atendimento.shared.exception.FieldErrorDetail;
import com.sea.atendimento.user.domain.Uf;
import com.sea.atendimento.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "solicitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Solicitation {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal HIGH_PRIORITY_MIN_VALUE = new BigDecimal("100");


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SolicitationStatus status;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    private ServiceType serviceType;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;


    @Embedded
    private Address address;


    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Priority priority;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "estimated_value", precision = 12, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyst_id")
    private User analyst;

    @Column(name = "decision_comment", length = 1000)
    private String decisionComment;


    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;


    public static Solicitation create(User client, ServiceType serviceType,
                                      String title, String description) {
        Solicitation solicitation = new Solicitation();
        solicitation.client = client;
        solicitation.status = SolicitationStatus.DRAFT;
        solicitation.currentStep = 1;
        solicitation.applyBasicData(serviceType, title, description);
        solicitation.createdAt = Instant.now();
        solicitation.updatedAt = solicitation.createdAt;
        return solicitation;
    }


    public void updateBasicData(ServiceType serviceType, String title, String description) {
        ensureEditable();
        applyBasicData(serviceType, title, description);
        touch();
    }

    public void updateAddress(Address address) {
        ensureEditable();
        this.address = address;
        this.currentStep = Math.max(currentStep, 2);
        touch();
    }

    public void updateConfirmation(Priority priority, LocalDate preferredDate,
                                   BigDecimal estimatedValue, boolean termsAccepted) {
        ensureEditable();
        if (currentStep < 2) {
            throw new BusinessRuleException("Conclua a etapa 2 (endereço) antes da etapa 3");
        }

        List<FieldErrorDetail> errors =
                confirmationErrors(priority, preferredDate, estimatedValue, termsAccepted);
        if (!errors.isEmpty()) {
            throw new BusinessRuleException("A etapa 3 possui dados inválidos", errors);
        }

        this.priority = priority;
        this.preferredDate = preferredDate;
        this.estimatedValue = estimatedValue;
        this.termsAccepted = termsAccepted;
        this.currentStep = 3;
        touch();
    }


    public void submit() {
        ensureEditable();

        List<FieldErrorDetail> errors = validateForSubmission();
        if (!errors.isEmpty()) {
            throw new BusinessRuleException("A solicitação possui etapas ou campos pendentes", errors);
        }

        this.status = SolicitationStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        touch();
    }

    public List<FieldErrorDetail> validateForSubmission() {
        List<FieldErrorDetail> errors = new ArrayList<>();

        if (address == null) {
            errors.add(FieldErrorDetail.ofStep(2, "address", "O endereço não foi informado"));
        }

        if (currentStep < 3) {
            errors.add(FieldErrorDetail.ofStep(3, "confirmation", "A etapa de confirmação não foi concluída"));
        } else {
            errors.addAll(confirmationErrors(priority, preferredDate, estimatedValue, termsAccepted));
        }

        return errors;
    }


    public void startReview(User analyst) {
        if (status != SolicitationStatus.SUBMITTED) {
            throw new ConflictException("Só é possível iniciar a análise de solicitações enviadas");
        }
        this.status = SolicitationStatus.IN_REVIEW;
        this.analyst = analyst;
        touch();
    }

    public void decide(User analyst, Decision decision, String comment) {
        if (status != SolicitationStatus.SUBMITTED && status != SolicitationStatus.IN_REVIEW) {
            throw new ConflictException("Esta solicitação não pode mais ser decidida");
        }
        this.status = decision == Decision.APPROVE
                ? SolicitationStatus.APPROVED
                : SolicitationStatus.REJECTED;
        this.analyst = analyst;
        this.decisionComment = comment.trim();
        this.decidedAt = Instant.now();
        touch();
    }


    public boolean isOwnedBy(Long userId) {
        return client.getId().equals(userId);
    }

    public Uf getUf() {
        return address == null ? null : address.getUf();
    }


    private void applyBasicData(ServiceType serviceType, String title, String description) {
        this.serviceType = serviceType;
        this.title = title.trim();
        this.description = description.trim();
    }

    private void ensureEditable() {
        if (status != SolicitationStatus.DRAFT) {
            throw new ConflictException("Só é possível editar solicitações em rascunho");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static List<FieldErrorDetail> confirmationErrors(Priority priority, LocalDate preferredDate,
                                                             BigDecimal estimatedValue, boolean termsAccepted) {
        List<FieldErrorDetail> errors = new ArrayList<>();

        if (priority == null) {
            errors.add(FieldErrorDetail.ofStep(3, "priority", "A prioridade é obrigatória"));
        }

        if (preferredDate == null) {
            errors.add(FieldErrorDetail.ofStep(3, "preferredDate", "A data preferencial é obrigatória"));
        } else if (preferredDate.isBefore(LocalDate.now(BUSINESS_ZONE))) {
            errors.add(FieldErrorDetail.ofStep(3, "preferredDate", "A data preferencial não pode estar no passado"));
        }

        if (estimatedValue == null) {
            errors.add(FieldErrorDetail.ofStep(3, "estimatedValue", "O valor estimado é obrigatório"));
        } else if (estimatedValue.signum() < 0) {
            errors.add(FieldErrorDetail.ofStep(3, "estimatedValue", "O valor estimado não pode ser negativo"));
        }

        if (!termsAccepted) {
            errors.add(FieldErrorDetail.ofStep(3, "termsAccepted", "Os termos precisam ser aceitos"));
        }

        if (priority == Priority.HIGH && estimatedValue != null
                && estimatedValue.compareTo(HIGH_PRIORITY_MIN_VALUE) < 0) {
            errors.add(FieldErrorDetail.ofStep(3, "estimatedValue",
                    "Para prioridade HIGH, o valor estimado deve ser de pelo menos 100"));
        }

        return errors;
    }
}