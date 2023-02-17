package io.github.lc.oss.commons.web.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;

import io.github.lc.oss.commons.testing.AbstractTest;

public class EnableCsrfProtectionTest extends AbstractTest {
    @Test
    public void test_matches_null() {
        this.test(null, true);
    }

    @Test
    public void test_matches_blank() {
        this.test(" ", true);
    }

    @Test
    public void test_matches_true() {
        this.test("true", true);
    }

    @Test
    public void test_matches_true_v2() {
        this.test("tRuE", true);
    }

    @Test
    public void test_matches_junk() {
        this.test("junk", true);
    }

    @Test
    public void test_matches_false() {
        this.test("false", false);
    }

    @Test
    public void test_matches_false_v2() {
        this.test("FALSE", false);
    }

    private void test(String prop, boolean expected) {
        ConditionContext context = Mockito.mock(ConditionContext.class);
        Environment env = Mockito.mock(Environment.class);

        EnableCsrfProtection condition = new EnableCsrfProtection();

        Mockito.when(context.getEnvironment()).thenReturn(env);
        Mockito.when(env.getProperty("application.security.enableCsrfProtection")).thenReturn(prop);

        boolean result = condition.matches(context, null);
        Assertions.assertEquals(expected, result);
    }
}
