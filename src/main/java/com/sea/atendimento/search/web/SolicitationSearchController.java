package com.sea.atendimento.search.web;

import com.sea.atendimento.search.service.SolicitationSearchService;
import com.sea.atendimento.search.web.dto.SearchResponseDTO;
import com.sea.atendimento.search.web.dto.SolicitationSearchRequestDTO;
import com.sea.atendimento.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SolicitationSearchController {

    private final SolicitationSearchService service;

    public SolicitationSearchController(SolicitationSearchService service) {
        this.service = service;
    }

    @GetMapping("/analyst/solicitations/search")
    public SearchResponseDTO search(@AuthenticationPrincipal AuthenticatedUser user,
                                    @Valid  @ParameterObject SolicitationSearchRequestDTO request) {
        return service.search(request, user);
    }
}