package com.sea.atendimento.solicitation.service;

import com.sea.atendimento.shared.audit.Audit;
import com.sea.atendimento.shared.exception.NotFoundException;
import com.sea.atendimento.shared.security.AuthenticatedUser;
import com.sea.atendimento.solicitation.domain.Solicitation;
import com.sea.atendimento.solicitation.domain.SolicitationStatus;
import com.sea.atendimento.solicitation.infra.SolicitationRepository;
import com.sea.atendimento.solicitation.web.dto.DecisionRequestDTO;
import com.sea.atendimento.solicitation.web.dto.SolicitationResponseDTO;
import com.sea.atendimento.user.domain.Role;
import com.sea.atendimento.user.domain.User;
import com.sea.atendimento.user.infra.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalystSolicitationService {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    public AnalystSolicitationService(SolicitationRepository solicitationRepository,
                                      UserRepository userRepository,
                                      ApplicationEventPublisher events) {
        this.solicitationRepository = solicitationRepository;
        this.userRepository = userRepository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public SolicitationResponseDTO findById(Long id, AuthenticatedUser user) {
        User analyst = loadUser(user);
        Solicitation solicitation = findAccessible(id, analyst);
        return SolicitationResponseDTO.from(solicitation);
    }

    @Transactional
    @Audit("START_REVIEW")
    public SolicitationResponseDTO startReview(Long id, AuthenticatedUser user) {
        User analyst = loadUser(user);
        Solicitation solicitation = findAccessible(id, analyst);
        solicitation.startReview(analyst);
        return publishAndRespond(solicitation);
    }

    @Transactional
    @Audit("DECIDE_SOLICITATION")
    public SolicitationResponseDTO decide(Long id, AuthenticatedUser user, DecisionRequestDTO request) {
        User analyst = loadUser(user);
        Solicitation solicitation = findAccessible(id, analyst);
        solicitation.decide(analyst, request.decision(), request.comment());
        return publishAndRespond(solicitation);
    }

    private User loadUser(AuthenticatedUser user) {
        return userRepository.findById(user.id())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private Solicitation findAccessible(Long id, User user) {
        return solicitationRepository.findById(id)
                .filter(solicitation -> solicitation.getStatus() != SolicitationStatus.DRAFT)
                .filter(solicitation -> user.getRole() == Role.ADMIN
                        || user.getCoveredUfs().contains(solicitation.getUf()))
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));
    }

    private SolicitationResponseDTO publishAndRespond(Solicitation solicitation) {
        events.publishEvent(new SolicitationChangedEvent(solicitation.getId()));
        return SolicitationResponseDTO.from(solicitation);
    }
}