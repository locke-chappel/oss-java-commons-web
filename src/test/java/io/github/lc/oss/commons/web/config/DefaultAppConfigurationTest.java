package io.github.lc.oss.commons.web.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.core.GrantedAuthorityDefaults;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.testing.AbstractMockTest;

public class DefaultAppConfigurationTest extends AbstractMockTest {
    @Test
    public void test_beans() {
        DefaultAppConfiguration config = new DefaultAppConfiguration();

        L10N l10n = config.l10n();

        Assertions.assertNotNull(config.commonAdvice());
        Assertions.assertNotNull(config.cookiePrefixParser());
        Assertions.assertNotNull(config.eTagAdvice());
        Assertions.assertNotNull(l10n);
        Assertions.assertNotNull(config.minifier(false));
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

        Assertions.assertFalse(l10n.isCaching());
        Method method = this.findMethod("getExternalL10NRoot", L10N.class);
        boolean canAccess = method.canAccess(l10n);
        if (!canAccess) {
            method.setAccessible(true);
        }
        try {
            Assertions.assertNull(method.invoke(l10n));
        } catch (IllegalAccessException | InvocationTargetException ex) {
            Assertions.fail("Unexpected exception", ex);
        }
        method.setAccessible(canAccess);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_configureDefaults() {
        ObjectPostProcessor<Object> objectPostProcessor = Mockito.mock(ObjectPostProcessor.class);
        AuthenticationManagerBuilder authenticationBuilder = Mockito.mock(AuthenticationManagerBuilder.class);
        @SuppressWarnings("rawtypes")
        ObjectProvider provider = Mockito.mock(ObjectProvider.class);
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Mockito.when(context.getBeanNamesForType(AuthorizationEventPublisher.class)).thenReturn(new String[0]);
        Mockito.when(context.getBeanNamesForType(GrantedAuthorityDefaults.class)).thenReturn(new String[0]);
        Mockito.when(context.getBeanProvider(Mockito.any(ResolvableType.class))).thenReturn(provider);

        HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
        http.setSharedObject(ApplicationContext.class, context);

        DefaultAppConfiguration config = new DefaultAppConfiguration();
        this.setField("enableCsrfProtection", true, config);

        try {
            config.configureDefaults(http);
        } catch (Exception ex) {
            Assertions.fail("Unexpected exception", ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_configureDefaults_noCsrf() {
        ObjectPostProcessor<Object> objectPostProcessor = Mockito.mock(ObjectPostProcessor.class);
        AuthenticationManagerBuilder authenticationBuilder = Mockito.mock(AuthenticationManagerBuilder.class);
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        @SuppressWarnings("rawtypes")
        ObjectProvider provider = Mockito.mock(ObjectProvider.class);
        Mockito.when(context.getBeanNamesForType(AuthorizationEventPublisher.class)).thenReturn(new String[0]);
        Mockito.when(context.getBeanNamesForType(GrantedAuthorityDefaults.class)).thenReturn(new String[0]);
        Mockito.when(context.getBeanProvider(Mockito.any(ResolvableType.class))).thenReturn(provider);

        HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
        http.setSharedObject(ApplicationContext.class, context);

        DefaultAppConfiguration config = new DefaultAppConfiguration();
        this.setField("enableCsrfProtection", false, config);

        try {
            config.configureDefaults(http);
        } catch (Exception ex) {
            Assertions.fail("Unexpected exception", ex);
        }
    }
}
