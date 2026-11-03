package com.sea.atendimento.user.domain;

import com.sea.atendimento.shared.domain.Role;
import com.sea.atendimento.shared.domain.Uf;
import com.sea.atendimento.shared.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_covered_ufs", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "uf", length = 2, nullable = false)
    private Set<Uf> coveredUfs = new HashSet<>();

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

    public static User newAnalyst(String name, String email, String passwordHash, Set<Uf> coveredUfs) {
        if (coveredUfs == null || coveredUfs.isEmpty()) {
            throw new BusinessRuleException("Um analista precisa cobrir pelo menos uma UF");
        }
        User user = new User(name, email, passwordHash, Role.ANALYST);
        user.coveredUfs.addAll(coveredUfs);
        return user;
    }

    public static User newAdmin(String name, String email, String passwordHash) {
        return new User(name, email, passwordHash, Role.ADMIN);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public Set<Uf> getCoveredUfs() {
        return Set.copyOf(coveredUfs);
    }
}