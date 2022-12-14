package com.github.lc.oss.commons.web.config;

public class EnableCsrfProtection extends PropertyConditional {
    @Override
    protected String getProperty() {
        return "application.security.enableCsrfProtection";
    }
}
