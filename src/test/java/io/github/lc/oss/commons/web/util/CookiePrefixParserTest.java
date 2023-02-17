package io.github.lc.oss.commons.web.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import io.github.lc.oss.commons.testing.AbstractMockTest;

public class CookiePrefixParserTest extends AbstractMockTest {
    @InjectMocks
    private CookiePrefixParser parser;

    @Test
    public void test_isHostCookie() {
        boolean result = this.parser.isHostCookie(null);
        Assertions.assertFalse(result);

        result = this.parser.isHostCookie("");
        Assertions.assertFalse(result);

        result = this.parser.isHostCookie(" \t \r \n \t ");
        Assertions.assertFalse(result);

        result = this.parser.isHostCookie("name");
        Assertions.assertFalse(result);

        result = this.parser.isHostCookie("__Secure-name");
        Assertions.assertFalse(result);

        result = this.parser.isHostCookie("__Host-name");
        Assertions.assertTrue(result);
    }

    @Test
    public void test_isSecureCookie() {
        boolean result = this.parser.isSecureCookie(null);
        Assertions.assertFalse(result);

        result = this.parser.isSecureCookie("");
        Assertions.assertFalse(result);

        result = this.parser.isSecureCookie(" \t \r \n \t ");
        Assertions.assertFalse(result);

        result = this.parser.isSecureCookie("name");
        Assertions.assertFalse(result);

        result = this.parser.isSecureCookie("__Secure-name");
        Assertions.assertTrue(result);

        result = this.parser.isSecureCookie("__Host-name");
        Assertions.assertFalse(result);
    }
}
