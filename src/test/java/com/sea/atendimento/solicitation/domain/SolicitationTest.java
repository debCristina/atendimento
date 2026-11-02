package com.sea.atendimento.solicitation.domain;

import com.sea.atendimento.shared.exception.BusinessRuleException;
import com.sea.atendimento.shared.exception.ConflictException;
import com.sea.atendimento.shared.exception.FieldErrorDetail;
import com.sea.atendimento.user.domain.Uf;
import com.sea.atendimento.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolicitationTest {

    private final User client = User.newClient("Débora", "debora@email.com", "hash");
    private final User analyst = User.newAnalyst("Ana", "ana@email.com", "hash", Set.of(Uf.SP));

    @Test
    @DisplayName("Uma nova solicitação nasce como rascunho, na etapa 1")
    void newSolicitationStartsAsDraft() {
        Solicitation solicitation = newSolicitation();

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.getCurrentStep()).isEqualTo(1);
        assertThat(solicitation.getAddress()).isNull();
    }

    @Test
    @DisplayName("O envio incompleto lista as pendências de cada etapa")
    void submitWithMissingStepsListsPendingSteps() {
        Solicitation solicitation = newSolicitation();

        assertThatThrownBy(solicitation::submit)
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    List<FieldErrorDetail> errors = ((BusinessRuleException) ex).getFieldErrors();
                    assertThat(errors).extracting(error -> error.step()).containsExactly(2, 3);
                });

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
    }

    @Test
    @DisplayName("A etapa 3 não pode ser salva antes da etapa 2")
    void confirmationBeforeAddressIsRejected() {
        Solicitation solicitation = newSolicitation();

        assertThatThrownBy(() -> solicitation.updateConfirmation(
                Priority.LOW, LocalDate.now().plusDays(5), new BigDecimal("200"), true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("etapa 2");
    }

    @Test
    @DisplayName("Prioridade HIGH exige valor estimado de pelo menos 100")
    void highPriorityRequiresMinimumValue() {
        Solicitation solicitation = newSolicitation();
        solicitation.updateAddress(address());

        assertThatThrownBy(() -> solicitation.updateConfirmation(
                Priority.HIGH, LocalDate.now().plusDays(5), new BigDecimal("50"), true))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(solicitation.getCurrentStep()).isEqualTo(2);
    }

    @Test
    @DisplayName("Corrigir o endereço não faz o progresso regredir")
    void editingAddressDoesNotRegressCurrentStep() {
        Solicitation solicitation = completeSolicitation();

        solicitation.updateAddress(address());

        assertThat(solicitation.getCurrentStep()).isEqualTo(3);
    }

    @Test
    @DisplayName("Com todas as etapas válidas, o envio muda o status para SUBMITTED")
    void completeSubmitChangesStatus() {
        Solicitation solicitation = completeSolicitation();

        solicitation.submit();

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.SUBMITTED);
        assertThat(solicitation.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("Uma solicitação enviada não pode mais ser editada")
    void submittedSolicitationCannotBeEdited() {
        Solicitation solicitation = completeSolicitation();
        solicitation.submit();

        assertThatThrownBy(() -> solicitation.updateAddress(address()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("O analista pode decidir direto de SUBMITTED, e a decisão é final")
    void decisionIsFinal() {
        Solicitation solicitation = completeSolicitation();
        solicitation.submit();

        solicitation.decide(analyst, Decision.APPROVE, "Tudo conferido e aprovado.");

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.APPROVED);
        assertThatThrownBy(() -> solicitation.decide(analyst, Decision.REJECT, "Mudei de ideia agora."))
                .isInstanceOf(ConflictException.class);
    }


    private Solicitation newSolicitation() {
        return Solicitation.create(client, ServiceType.INSTALLATION,
                "Instalação de ar", "Instalar um aparelho split na sala de estar");
    }

    private Solicitation completeSolicitation() {
        Solicitation solicitation = newSolicitation();
        solicitation.updateAddress(address());
        solicitation.updateConfirmation(Priority.MEDIUM, LocalDate.now().plusDays(10),
                new BigDecimal("350"), true);
        return solicitation;
    }

    private Address address() {
        return new Address("01310100", "Avenida Paulista", "Bela Vista", "São Paulo",
                Uf.SP, "1000", null);
    }
}