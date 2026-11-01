package com.sea.atendimento.user.service;

import com.sea.atendimento.shared.exception.ConflictException;
import com.sea.atendimento.user.domain.User;
import com.sea.atendimento.user.dto.RegisterResponseDTO;
import com.sea.atendimento.user.infra.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sea.atendimento.user.dto.RegisterRequestDTO;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}