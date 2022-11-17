package com.github.lc.oss.commons.web.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import com.github.lc.oss.commons.l10n.L10N;
import com.github.lc.oss.commons.testing.AbstractMockTest;

public class DefaultAppConfigurationTest extends AbstractMockTest {
    @Test
    public void test_beans() {
        DefaultAppConfiguration config = new DefaultAppConfiguration();

        L10N l10n = config.l10n();

        Assertions.assertNotNull(config.clock());
        Assertions.assertNotNull(config.commonAdvice());
        Assertions.assertNotNull(config.cookiePrefixParser());
        Assertions.assertNotNull(config.eTagAdvice());
        Assertions.assertNotNull(config.exceptionController());
        Assertions.assertNotNull(l10n);
        Assertions.assertNotNull(config.minifier(false));
        Assertions.assertNotNull(config.pathNormalizer());
        Assertions.assertNotNull(config.trimmingModule());
        Assertions.assertNotNull(config.userLocale());
        Assertions.assertNotNull(config.userLocaleFilter());
        Assertions.assertNotNull(config.themeService());
        Assertions.assertNotNull(config.userTheme());
        Assertions.assertNotNull(config.userThemeFilter());
        Assertions.assertNotNull(config.libThemeResourceFileResolver());
        Assertions.assertNotNull(config.appThemeResourceFileResolver("app", 1));
        Assertions.assertNotNull(config.extThemeResourceFileResolver("ext", 1));
        Assertions.assertNotNull(config.csrfFilter());
        Assertions.assertNotNull(config.csrfTokenManager());
        Assertions.assertNotNull(config.securityHeadersFilter());
        Assertions.assertNotNull(config.sessionRegistry());

        Assertions.assertFalse(l10n.isCaching());
        Method method = this.findMethod("getExternalL10NRoot", L10N.class);
        boolean canAccess = method.canAccess(l10n);
        if (!canAccess) {
            method.setAccessible(true);
        }
        try {
            Assertions.assertNull(method.invoke(l10n));
        } catch (IllegalAccessException | InvocationTargetException ex) {
            Assertions.fail("Unexpected exception");
        }
        method.setAccessible(canAccess);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test_configurePathMatch() {
        PathMatchConfigurer configurer = Mockito.mock(PathMatchConfigurer.class);

        DefaultAppConfiguration config = new DefaultAppConfiguration();

        Mockito.when(configurer.setUseSuffixPatternMatch(false)).thenReturn(null);

        config.configurePathMatch(configurer);
    }

    @Test
    public void test_configureDefaults() {
        @SuppressWarnings("unchecked")
        ObjectPostProcessor<Object> objectPostProcessor = Mockito.mock(ObjectPostProcessor.class);
        AuthenticationManagerBuilder authenticationBuilder = Mockito.mock(AuthenticationManagerBuilder.class);
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Mockito.when(context.getBeanNamesForType(GrantedAuthorityDefaults.class)).thenReturn(new String[0]);

        HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
        http.setSharedObject(ApplicationContext.class, context);

        DefaultAppConfiguration config = new DefaultAppConfiguration();
        this.setField("enableCsrfProtection", true, config);

        try {
            config.configureDefaults(http);
        } catch (Exception ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_configureDefaults_noCsrf() {
        @SuppressWarnings("unchecked")
        ObjectPostProcessor<Object> objectPostProcessor = Mockito.mock(ObjectPostProcessor.class);
        AuthenticationManagerBuilder authenticationBuilder = Mockito.mock(AuthenticationManagerBuilder.class);
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Mockito.when(context.getBeanNamesForType(GrantedAuthorityDefaults.class)).thenReturn(new String[0]);

        HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
        http.setSharedObject(ApplicationContext.class, context);

        DefaultAppConfiguration config = new DefaultAppConfiguration();
        this.setField("enableCsrfProtection", false, config);

        try {
            config.configureDefaults(http);
        } catch (Exception ex) {
            Assertions.fail("Unexpected exception");
        }
    }
}
