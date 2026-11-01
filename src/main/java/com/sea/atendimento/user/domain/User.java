package com.sea.atendimento.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

  public static String normalizeEmail(String email) {
    return email.trim().toLowerCase();
}

private User(String name, String email, String passwordHash, Role role) {
    this.name = name.trim();
    this.email = normalizeEmail(email);  
    this.passwordHash = passwordHash;
    this.role = role;
    this.createdAt = Instant.now();
}

    public static User newClient(String name, String email, String passwordHash) {
        return new User(name, email, passwordHash, Role.CLIENT);
    }
}