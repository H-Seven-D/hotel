package com.homestay.system.log;

import com.homestay.security.AuthUser;
import com.homestay.security.SecurityUtils;
import com.homestay.system.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditActionAspect {

    private final AuditService auditService;

    @Around("@annotation(auditAction)")
    public Object recordOperation(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        String result = "SUCCESS";
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            result = "FAIL";
            throw throwable;
        } finally {
            HttpServletRequest request = currentRequest();
            AuthUser user = currentUserSafely();
            auditService.record(
                    user,
                    auditAction.module(),
                    auditAction.action(),
                    request == null ? "" : request.getMethod(),
                    request == null ? "" : request.getRequestURI(),
                    request == null ? "" : clientIp(request),
                    result
            );
        }
    }

    private AuthUser currentUserSafely() {
        try {
            return SecurityUtils.currentUser();
        } catch (Exception ignored) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
