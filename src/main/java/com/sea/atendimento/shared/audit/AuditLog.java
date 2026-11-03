package com.sea.atendimento.shared.audit;

import com.sea.atendimento.shared.domain.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    private static final int MAX_REASON_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", length = 20)
    private Role userRole;

    @Column(name = "resource_id", length = 50)
    private String resourceId;

    @Column(nullable = false, length = 10)
    private String outcome;

    @Column(length = MAX_REASON_LENGTH)
    private String reason;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static AuditLog success(String action, Long userId, Role userRole,
                                   String resourceId, long durationMs) {
        return create(action, userId, userRole, resourceId, "SUCCESS", null, durationMs);
    }

    public static AuditLog failure(String action, Long userId, Role userRole,
                                   String resourceId, String reason, long durationMs) {
        return create(action, userId, userRole, resourceId, "FAILURE", truncate(reason), durationMs);
    }

    private static AuditLog create(String action, Long userId, Role userRole, String resourceId,
                                   String outcome, String reason, long durationMs) {
        AuditLog log = new AuditLog();
        log.action = action;
        log.userId = userId;
        log.userRole = userRole;
        log.resourceId = resourceId;
        log.outcome = outcome;
        log.reason = reason;
        log.durationMs = durationMs;
        log.createdAt = Instant.now();
        return log;
    }

    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_REASON_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_REASON_LENGTH);
    }
}