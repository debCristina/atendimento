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

    @Around("@annotation(auditAnnotation)")
    public Object audit(ProceedingJoinPoint joinPoint, Audit auditAnnotation) throws Throwable {
        String action = auditAnnotation.value();
        String user = currentUser();
        String resource = resourceId(joinPoint.getArgs());
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            audit.info("action={} user={} resource={} outcome=SUCCESS durationMs={}",
                    action, user, resource, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            audit.warn("action={} user={} resource={} outcome=FAILURE reason=\"{}\" durationMs={}",
                    action, user, resource, ex.getMessage(), System.currentTimeMillis() - start);
            throw ex;
        }
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.id() + ":" + user.role();
        }
        return "anonymous";
    }

    private String resourceId(Object[] args) {
        return Arrays.stream(args)
                .filter(Long.class::isInstance)
                .findFirst()
                .map(String::valueOf)
                .orElse("-");
    }
}