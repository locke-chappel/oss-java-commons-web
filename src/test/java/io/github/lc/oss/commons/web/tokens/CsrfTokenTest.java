package io.github.lc.oss.commons.web.tokens;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.commons.testing.AbstractTest;

public class CsrfTokenTest extends AbstractTest {
    @Test
    public void test_constructors() {
        CsrfToken token1 = new CsrfToken(0);
        CsrfToken token2 = new CsrfToken(1000);
        CsrfToken token3 = new CsrfToken("id", 0);
        CsrfToken token4 = new CsrfToken("id", -1);

        Assertions.assertNotNull(token1.getId());
        Assertions.assertTrue(token1.getExpires() <= System.currentTimeMillis());

        Assertions.assertNotNull(token2.getId());
        Assertions.assertTrue(token2.getExpires() > System.currentTimeMillis());

        Assertions.assertNotEquals(token1.getId(), token2.getId());

        Assertions.assertEquals("id", token3.getId());
        Assertions.assertEquals(0, token3.getExpires());

        Assertions.assertEquals("id", token4.getId());
        Assertions.assertEquals(-1, token4.getExpires());
    }
}
