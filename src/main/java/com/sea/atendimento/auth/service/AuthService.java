package com.sea.atendimento.auth.service;

import com.sea.atendimento.auth.web.dto.LoginRequestDTO;
import com.sea.atendimento.auth.web.dto.RegisterRequestDTO;
import com.sea.atendimento.auth.web.dto.TokenResponseDTO;
import com.sea.atendimento.shared.exception.ConflictException;
import com.sea.atendimento.shared.exception.InvalidCredentialsException;
import com.sea.atendimento.shared.security.JwtService;
import com.sea.atendimento.user.domain.User;
import com.sea.atendimento.user.infra.UserRepository;
import com.sea.atendimento.user.web.dto.RegisterResponseDTO;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


      public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        String email = User.normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = User.newClient(request.name(), email, passwordHash);

        User saved = userRepository.save(user);
        return RegisterResponseDTO.from(saved);
    }

     @Transactional(readOnly = true)
    public TokenResponseDTO login(LoginRequestDTO request) {
        String email = User.normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.generateToken(user);
        return TokenResponseDTO.bearer(token, jwtService.getExpiration());
    }

    
}