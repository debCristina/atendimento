package com.sea.atendimento.solicitation.web;

import com.sea.atendimento.shared.security.AuthenticatedUser;
import com.sea.atendimento.solicitation.service.SolicitationService;
import com.sea.atendimento.solicitation.web.dto.AddressRequestDTO;
import com.sea.atendimento.solicitation.web.dto.BasicDataRequestDTO;
import com.sea.atendimento.solicitation.web.dto.ConfirmationRequestDTO;
import com.sea.atendimento.solicitation.web.dto.SolicitationResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/solicitations")
public class ClientSolicitationController {

    private final SolicitationService service;

    public ClientSolicitationController(SolicitationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitationResponseDTO create(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody BasicDataRequestDTO request) {
        return service.create(user.id(), request);
    }

    @GetMapping
    public List<SolicitationResponseDTO> listMine(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listMine(user.id());
    }

    @GetMapping("/{id}")
    public SolicitationResponseDTO findMine(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable Long id) {
        return service.findMine(id, user.id());
    }

    @PutMapping("/{id}/basic-data")
    public SolicitationResponseDTO updateBasicData(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody BasicDataRequestDTO request) {
        return service.updateBasicData(id, user.id(), request);
    }

    @PutMapping("/{id}/address")
    public SolicitationResponseDTO updateAddress(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody AddressRequestDTO request) {
        return service.updateAddress(id, user.id(), request);
    }

    @PutMapping("/{id}/confirmation")
    public SolicitationResponseDTO updateConfirmation(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody ConfirmationRequestDTO request) {
        return service.updateConfirmation(id, user.id(), request);
    }

    @PostMapping("/{id}/submit")
    public SolicitationResponseDTO submit(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable Long id) {
        return service.submit(id, user.id());
    }
}
