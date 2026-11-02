package com.sea.atendimento.solicitation.service;

import com.sea.atendimento.shared.exception.NotFoundException;
import com.sea.atendimento.shared.security.AuthenticatedUser;
import com.sea.atendimento.solicitation.domain.*;
import com.sea.atendimento.solicitation.infra.SolicitationRepository;
import com.sea.atendimento.solicitation.web.dto.SolicitationResponseDTO;
import com.sea.atendimento.user.domain.Role;
import com.sea.atendimento.user.domain.Uf;
import com.sea.atendimento.user.domain.User;
import com.sea.atendimento.user.infra.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalystSolicitationServiceTest {

    private static final Long SOLICITATION_ID = 1L;
    private static final Long USER_ID = 10L;

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private AnalystSolicitationService service;

    private final AuthenticatedUser authenticated = new AuthenticatedUser(USER_ID, Role.ANALYST);

    @Test
    @DisplayName("O analista vê uma solicitação da UF que ele cobre")
    void analystSeesSolicitationFromCoveredUf() {
        givenUser(analystCovering(Uf.SP));
        givenSolicitation(submittedIn(Uf.SP));

        SolicitationResponseDTO response = service.findById(SOLICITATION_ID, authenticated);

        assertThat(response.status()).isEqualTo(SolicitationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("O analista recebe 404 para uma solicitação de outra UF")
    void analystCannotSeeSolicitationFromOtherUf() {
        givenUser(analystCovering(Uf.MG));
        givenSolicitation(submittedIn(Uf.SP));

        assertThatThrownBy(() -> service.findById(SOLICITATION_ID, authenticated))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("O admin vê solicitações de qualquer UF")
    void adminSeesAnyUf() {
        givenUser(User.newAdmin("Admin", "admin@email.com", "hash"));
        givenSolicitation(submittedIn(Uf.SP));

        SolicitationResponseDTO response = service.findById(SOLICITATION_ID, authenticated);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Rascunhos não são visíveis para a análise")
    void draftsAreNotVisible() {
        givenUser(analystCovering(Uf.SP));
        givenSolicitation(draft());

        assertThatThrownBy(() -> service.findById(SOLICITATION_ID, authenticated))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Iniciar a análise muda o status e publica o evento de indexação")
    void startReviewChangesStatusAndPublishesEvent() {
        givenUser(analystCovering(Uf.SP));
        givenSolicitation(submittedIn(Uf.SP));

        SolicitationResponseDTO response = service.startReview(SOLICITATION_ID, authenticated);

        assertThat(response.status()).isEqualTo(SolicitationStatus.IN_REVIEW);
        verify(events).publishEvent(any(SolicitationChangedEvent.class));
    }

    @Test
    @DisplayName("Uma análise negada pela UF não publica evento")
    void deniedActionDoesNotPublishEvent() {
        givenUser(analystCovering(Uf.MG));
        givenSolicitation(submittedIn(Uf.SP));

        assertThatThrownBy(() -> service.startReview(SOLICITATION_ID, authenticated))
                .isInstanceOf(NotFoundException.class);

        verify(events, never()).publishEvent(any());
    }


    private void givenUser(User user) {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private void givenSolicitation(Solicitation solicitation) {
        when(solicitationRepository.findById(SOLICITATION_ID)).thenReturn(Optional.of(solicitation));
    }

    private User analystCovering(Uf uf) {
        return User.newAnalyst("Analista", "analista@email.com", "hash", Set.of(uf));
    }

    private Solicitation draft() {
        User client = User.newClient("Cliente", "cliente@email.com", "hash");
        return Solicitation.create(client, ServiceType.INSTALLATION,
                "Instalação de ar", "Instalar um aparelho split na sala de estar");
    }

    private Solicitation submittedIn(Uf uf) {
        Solicitation solicitation = draft();
        solicitation.updateAddress(new Address("01310100", "Rua", "Bairro", "Cidade", uf, "10", null));
        solicitation.updateConfirmation(Priority.MEDIUM, LocalDate.now().plusDays(10),
                new BigDecimal("350"), true);
        solicitation.submit();
        return solicitation;
    }
}
