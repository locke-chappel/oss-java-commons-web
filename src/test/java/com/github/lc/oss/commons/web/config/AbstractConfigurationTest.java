package com.github.lc.oss.commons.web.config;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import com.github.lc.oss.commons.testing.AbstractMockTest;

public class AbstractConfigurationTest extends AbstractMockTest {
    private static class TestConfig extends AbstractConfiguration {

    }

    @Test
    public void test_beans() {
        AbstractConfiguration config = new TestConfig();

        Assertions.assertNotNull(config.clock());
        Assertions.assertNotNull(config.exceptionController());
        Assertions.assertNotNull(config.pathNormalizer());
        Assertions.assertNotNull(config.trimmingModule());
        Assertions.assertNotNull(config.securityHeadersFilter());
        Assertions.assertNotNull(config.sessionRegistry());
    }

    @Test
    public void test_configureDefaultPublicAccessUrls() {
        AbstractConfiguration config = new TestConfig();

        @SuppressWarnings("unchecked")
        HttpSecurity http = new HttpSecurity(Mockito.mock(ObjectPostProcessor.class), Mockito.mock(AuthenticationManagerBuilder.class), new HashMap<>());

        try {
            config.configureDefaultPublicAccessUrls(http);
        } catch (Exception ex) {
            Assertions.fail("Unexpected exception", ex);
        }
    }

    @Test
    public void test_configurePathMatch() {
        PathMatchConfigurer configurer = Mockito.mock(PathMatchConfigurer.class);

        AbstractConfiguration config = new TestConfig();

        config.configurePathMatch(configurer);
    }
}
