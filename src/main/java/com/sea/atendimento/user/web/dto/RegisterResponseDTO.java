package com.sea.atendimento.user.dto;

import com.sea.atendimento.user.domain.Role;
import com.sea.atendimento.user.domain.User;

import java.time.Instant;

public record RegisterResponseDTO(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt
) {
    public static RegisterResponseDTO from(User user) {
        return new RegisterResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}