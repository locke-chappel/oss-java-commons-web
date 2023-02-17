package io.github.lc.oss.commons.web.tokens;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.encryption.Ciphers;
import io.github.lc.oss.commons.testing.AbstractTest;

public class EncryptedStatelessCsrfTokenManagerTest extends AbstractTest {
    private EncryptedStatelessCsrfTokenManager manager;

    @BeforeEach
    public void init() {
        this.manager = new EncryptedStatelessCsrfTokenManager();
        this.setField("sessionCookieName", "cookie", this.manager);
        this.setField("secureCookies", true, this.manager);
        this.setField("cookiePath", "/", this.manager);
        this.setField("cookieDomain", "localhost", this.manager);
    }

    @Test
    public void test_ciphering() {
        Token token = new CsrfToken(300000);

        String cipher = this.manager.toJson(token);
        Assertions.assertNotNull(cipher);
        Assertions.assertNotEquals("", cipher.trim());
        Assertions.assertFalse(cipher.contains("{"));
        Token clearToken = this.manager.fromJson(cipher);
        Assertions.assertEquals(token.getId(), clearToken.getId());
        Assertions.assertEquals(token.getExpires(), clearToken.getExpires());
        Assertions.assertNotSame(token, clearToken);
    }

    @Test
    public void test_fromJson_blanks() {
        Token result = this.manager.fromJson(null);
        Assertions.assertNull(result);

        result = this.manager.fromJson("");
        Assertions.assertNull(result);

        result = this.manager.fromJson(" ");
        Assertions.assertNull(result);

        result = this.manager.fromJson(" \t \r \n \t ");
        Assertions.assertNull(result);
    }

    @Test
    public void test_fromJson_exception() {
        String cipheredBadData = Ciphers.AES128.encrypt(Encodings.Base64.encode("bad-json"), this.manager.getCipherKey());

        try {
            this.manager.fromJson(cipheredBadData);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error reading Token JSON.", ex.getMessage());
        }
    }

    @Test
    public void test_toJson_nullToken() {
        Assertions.assertNull(this.manager.toJson(null));
    }

    @Test
    public void test_toJson_exception() {
        Token badToken = new Token() {

            @Override
            public String getId() {
                throw new RuntimeException("Boom!");
            }

            @Override
            public long getExpires() {
                return 0;
            }
        };

        try {
            this.manager.toJson(badToken);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error writing Token JSON.", ex.getMessage());
        }
    }
}
