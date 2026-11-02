package com.sea.atendimento.auth.web;


import com.sea.atendimento.auth.service.AuthService;
import com.sea.atendimento.auth.web.dto.LoginRequestDTO;
import com.sea.atendimento.auth.web.dto.RegisterRequestDTO;
import com.sea.atendimento.auth.web.dto.TokenResponseDTO;
import com.sea.atendimento.user.web.dto.UserResponseDTO;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
        return authService.register(request);
    }

     @PostMapping("/login")
    public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }
}