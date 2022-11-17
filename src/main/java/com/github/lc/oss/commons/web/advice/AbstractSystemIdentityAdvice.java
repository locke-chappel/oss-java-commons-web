package com.github.lc.oss.commons.web.advice;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public abstract class AbstractSystemIdentityAdvice {
    protected abstract Authentication getSystemAuthentication();

    @Pointcut("@annotation(com.github.lc.oss.commons.web.annotations.SystemIdentity)")
    public void systemIdentity() {
    }

    @Around("systemIdentity()")
    public Object authorizeSystem(final ProceedingJoinPoint method) {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        try {
            Authentication systemUser = this.getSystemAuthentication();
            if (systemUser == null) {
                throw new RuntimeException("Unable to authorize system user because it is null");
            }
            SecurityContextHolder.getContext().setAuthentication(systemUser);

            Object value;
            try {
                value = method.proceed();
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }

            return value;
        } finally {
            SecurityContextHolder.getContext().setAuthentication(currentUser);
        }
    }
}
