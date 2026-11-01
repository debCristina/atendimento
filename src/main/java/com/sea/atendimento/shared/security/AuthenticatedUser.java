package com.sea.atendimento.shared.security;

import com.sea.atendimento.user.domain.Role;

public record AuthenticatedUser(Long id, Role role) { }