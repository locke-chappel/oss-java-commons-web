package com.github.lc.oss.commons.web.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class EnableCsrfProtection implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String property = context.getEnvironment().getProperty("application.security.enableCsrfProtection");
        if (property == null) {
            return true;
        }
        return !property.toLowerCase().equals("false");
    }
}
