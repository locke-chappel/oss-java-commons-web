package com.github.lc.oss.commons.web.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.lc.oss.commons.testing.AbstractTest;

public class AuthoritiesTest extends AbstractTest {
    @Test
    public void test_defaults() {
        new Authorities();

        Assertions.assertEquals("permitAll", Authorities.PUBLIC);
        Assertions.assertEquals("denyAll", Authorities.FORBIDDEN);
    }
}
