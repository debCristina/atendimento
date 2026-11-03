package com.sea.atendimento.user.web.dto;

import com.sea.atendimento.shared.domain.Role;
import com.sea.atendimento.shared.domain.Uf;
import com.sea.atendimento.user.domain.User;

import java.time.Instant;
import java.util.Set;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        Role role,
        Set<Uf> coveredUfs,
        Instant createdAt
) {
    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCoveredUfs(),
                user.getCreatedAt()
        );
    }
}