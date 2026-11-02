package com.sea.atendimento.solicitation.web;

import com.sea.atendimento.shared.security.AuthenticatedUser;
import com.sea.atendimento.solicitation.service.AnalystSolicitationService;
import com.sea.atendimento.solicitation.web.dto.DecisionRequestDTO;
import com.sea.atendimento.solicitation.web.dto.SolicitationResponseDTO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analyst/solicitations")
public class AnalystSolicitationController {

    private final AnalystSolicitationService service;

    public AnalystSolicitationController(AnalystSolicitationService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public SolicitationResponseDTO findById(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable Long id) {
        return service.findById(id, user);
    }

    @PostMapping("/{id}/start")
    public SolicitationResponseDTO startReview(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable Long id) {
        return service.startReview(id, user);
    }

    @PostMapping("/{id}/decide")
    public SolicitationResponseDTO decide(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable Long id,
                                          @Valid @RequestBody DecisionRequestDTO request) {
        return service.decide(id, user, request);
    }
}