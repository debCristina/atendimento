package com.sea.atendimento.user.infra;

import com.sea.atendimento.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sea.atendimento.user.domain.Role;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}