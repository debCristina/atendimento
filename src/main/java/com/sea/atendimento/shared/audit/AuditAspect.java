package com.sea.atendimento.shared.audit;

import com.sea.atendimento.shared.security.AuthenticatedUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Order(0)
public class AuditAspect {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");
    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository repository;

    public AuditAspect(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Around("@annotation(auditAnnotation)")
    public Object audit(ProceedingJoinPoint joinPoint, Audit auditAnnotation) throws Throwable {
        String action = auditAnnotation.value();
        AuthenticatedUser user = currentUser();
        String resource = resourceId(joinPoint.getArgs());
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            audit.info("action={} user={} resource={} outcome=SUCCESS durationMs={}",
                    action, describe(user), resource, duration);
            save(AuditLog.success(action, idOf(user), roleOf(user), resource, duration));
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            audit.warn("action={} user={} resource={} outcome=FAILURE reason=\"{}\" durationMs={}",
                    action, describe(user), resource, ex.getMessage(), duration);
            save(AuditLog.failure(action, idOf(user), roleOf(user), resource, ex.getMessage(), duration));
            throw ex;
        }
    }

    private void save(AuditLog entry) {
        try {
            repository.save(entry);
        } catch (Exception e) {
            log.error("Não foi possível gravar o registro de auditoria", e);
        }
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    private String describe(AuthenticatedUser user) {
        return user == null ? "anonymous" : user.id() + ":" + user.role();
    }

    private Long idOf(AuthenticatedUser user) {
        return user == null ? null : user.id();
    }

    private com.sea.atendimento.shared.domain.Role roleOf(AuthenticatedUser user) {
        return user == null ? null : user.role();
    }

    private String resourceId(Object[] args) {
        return Arrays.stream(args)
                .filter(Long.class::isInstance)
                .findFirst()
                .map(String::valueOf)
                .orElse(null);
    }
}