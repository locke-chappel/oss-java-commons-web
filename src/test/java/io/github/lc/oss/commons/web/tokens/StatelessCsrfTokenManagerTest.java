package io.github.lc.oss.commons.web.tokens;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.hashing.Hashes;
import io.github.lc.oss.commons.testing.AbstractTest;
import io.github.lc.oss.commons.web.util.CookiePrefixParser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class StatelessCsrfTokenManagerTest extends AbstractTest {
    private StatelessCsrfTokenManager manager;

    private CookiePrefixParser cookiePrefixParser = new CookiePrefixParser();

    @BeforeEach
    public void init() {
        this.manager = new StatelessCsrfTokenManager();
        this.setField("sessionCookieName", "cookie", this.manager);
        this.setField("secureCookies", true, this.manager);
        this.setField("cookiePath", "/", this.manager);
        this.setField("cookieDomain", "localhost", this.manager);
        this.setField("cookiePrefixParser", this.cookiePrefixParser, this.manager);
    }

    @Test
    public void test_getHeaderId() {
        Assertions.assertEquals("X-CSRF", this.manager.getHeaderId());
    }

    @Test
    public void test_getCsrfCookieId() {
        String result = this.manager.getCookieId();
        Assertions.assertEquals("csrf", result);

        String result2 = this.manager.getCookieId();
        Assertions.assertSame(result, result2);

        this.setField("csrfCookieId", null, this.manager);
        this.setField("sessionCookieName", "__Secure-cookie", this.manager);
        this.setField("secureCookies", true, this.manager);
        String result3 = this.manager.getCookieId();
        Assertions.assertEquals("__Secure-csrf", result3);
        Assertions.assertEquals(true, this.getField("secureCookies", this.manager));

        this.setField("csrfCookieId", null, this.manager);
        this.setField("sessionCookieName", "__Secure-cookie", this.manager);
        this.setField("secureCookies", false, this.manager);
        String result4 = this.manager.getCookieId();
        Assertions.assertEquals("__Secure-csrf", result4);
        Assertions.assertEquals(true, this.getField("secureCookies", this.manager));

        this.setField("csrfCookieId", null, this.manager);
        this.setField("sessionCookieName", null, this.manager);
        this.setField("secureCookies", false, this.manager);
        String result5 = this.manager.getCookieId();
        Assertions.assertEquals("csrf", result5);

        this.setField("csrfCookieId", null, this.manager);
        this.setField("sessionCookieName", null, this.manager);
        this.setField("secureCookies", true, this.manager);
        String result6 = this.manager.getCookieId();
        Assertions.assertEquals("csrf", result6);

        this.setField("csrfCookieId", null, this.manager);
        this.setField("sessionCookieName", "__Host-cookie", this.manager);
        this.setField("secureCookies", true, this.manager);
        String result7 = this.manager.getCookieId();
        Assertions.assertEquals("__Host-csrf", result7);
        Assertions.assertEquals(true, this.getField("secureCookies", this.manager));

        this.setField("csrfCookieId", null, this.manager);
        this.setField("sessionCookieName", "__Host-cookie", this.manager);
        this.setField("secureCookies", false, this.manager);
        String result8 = this.manager.getCookieId();
        Assertions.assertEquals("__Host-csrf", result8);
        Assertions.assertEquals(true, this.getField("secureCookies", this.manager));
    }

    @Test
    public void test_getCsrfHeaderCookieId() {
        String result = this.manager.getHeaderCookieId();
        Assertions.assertEquals("X-CSRF", result);

        String result2 = this.manager.getHeaderCookieId();
        Assertions.assertSame(result, result2);

        this.setField("csrfHeaderCookieId", null, this.manager);
        this.setField("sessionCookieName", "__Secure-cookie", this.manager);
        this.setField("secureCookies", true, this.manager);
        String result3 = this.manager.getHeaderCookieId();
        Assertions.assertEquals("__Secure-X-CSRF", result3);
        Assertions.assertEquals(true, this.getField("secureCookies", this.manager));

        this.setField("csrfHeaderCookieId", null, this.manager);
        this.setField("sessionCookieName", "__Secure-cookie", this.manager);
        this.setField("secureCookies", false, this.manager);
        String result4 = this.manager.getHeaderCookieId();
        Assertions.assertEquals("__Secure-X-CSRF", result4);
        Assertions.assertEquals(true, this.getField("secureCookies", this.manager));

        this.setField("csrfHeaderCookieId", null, this.manager);
        this.setField("sessionCookieName", null, this.manager);
        this.setField("secureCookies", false, this.manager);
        String result5 = this.manager.getHeaderCookieId();
        Assertions.assertEquals("X-CSRF", result5);

        this.setField("csrfHeaderCookieId", null, this.manager);
        this.setField("sessionCookieName", null, this.manager);
        this.setField("secureCookies", true, this.manager);
        String result6 = this.manager.getHeaderCookieId();
        Assertions.assertEquals("X-CSRF", result6);

        this.setField("csrfHeaderCookieId", null, this.manager);
        this.setField("sessionCookieName", "__Host-cookie", this.manager);
        this.setField("secureCookies", true, this.manager);
        String result7 = this.manager.getHeaderCookieId();
        Assertions.assertEquals("__Host-X-CSRF", result7);
        Assertions.assertEquals(true, this.getField("secureCookies", this.manager));

        this.setField("csrfHeaderCookieId", null, this.manager);
        this.setField("sessionCookieName", "__Host-cookie", this.manager);
        this.setField("secureCookies", false, this.manager);
        String result8 = this.manager.getHeaderCookieId();
        Assertions.assertEquals("__Host-X-CSRF", result8);
        Assertions.assertEquals(true, this.getField("secureCookies", this.manager));

    }

    @Test
    public void test_setToken() {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Cookie c = invocation.getArgument(0);
                Assertions.assertEquals("localhost", c.getDomain());
                Assertions.assertEquals("/", c.getPath());
                Assertions.assertEquals(300, c.getMaxAge());
                Assertions.assertTrue(c.getSecure());
                if ("X-CSRF".equals(c.getName())) {
                    Assertions.assertFalse(c.isHttpOnly());
                } else if ("csrf".equals(c.getName())) {
                    Assertions.assertTrue(c.isHttpOnly());
                } else {
                    Assertions.fail("Unexpceted cookie");
                }
                return null;
            }
        }).when(response).addCookie(ArgumentMatchers.any());

        this.manager.setToken(response);
    }

    @Test
    public void test_isValid_nullHeader_nullCookie() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getHeader("X-CSRF")).thenReturn(null);
        Mockito.when(request.getCookies()).thenReturn(null);

        boolean result = this.manager.isValid(request);
        Assertions.assertFalse(result);
    }

    @Test
    public void test_isValid_nullHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getHeader("X-CSRF")).thenReturn(null);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(this.manager.getCookieId(), "") });

        boolean result = this.manager.isValid(request);
        Assertions.assertFalse(result);
    }

    @Test
    public void test_isValid_nullCookie() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getHeader("X-CSRF")).thenReturn("value");
        Mockito.when(request.getCookies()).thenReturn(null);

        boolean result = this.manager.isValid(request);
        Assertions.assertFalse(result);
    }

    @Test
    public void test_isValid_nullCookie_v2() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getHeader("X-CSRF")).thenReturn("value");
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] {});

        boolean result = this.manager.isValid(request);
        Assertions.assertFalse(result);
    }

    @Test
    public void test_isValid_nullCookie_v3() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getHeader("X-CSRF")).thenReturn("value");
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("something", "v") });

        boolean result = this.manager.isValid(request);
        Assertions.assertFalse(result);
    }

    @Test
    public void test_isValid_badToken() {
        Token token = new CsrfToken(3000);
        String cipher = this.manager.toJson(token);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        /* note: wrong has on purpose */
        Mockito.when(request.getHeader("X-CSRF")).thenReturn(Hashes.SHA1.hash(cipher, Encodings.Base64));
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(this.manager.getCookieId(), cipher) });

        boolean result = this.manager.isValid(request);
        Assertions.assertFalse(result);
    }

    @Test
    public void test_isValid_nullToken() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getHeader("X-CSRF")).thenReturn(Hashes.SHA2_256.hash(this.manager.getSalt() + "", Encodings.Base64));
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(this.manager.getCookieId(), "") });

        boolean result = this.manager.isValid(request);
        Assertions.assertFalse(result);
    }

    @Test
    public void test_isValid_expired() {
        Token token = new CsrfToken(-3000);
        String json = this.manager.toJson(token);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getHeader("X-CSRF")).thenReturn(Hashes.SHA2_256.hash(this.manager.getSalt() + json, Encodings.Base64));
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(this.manager.getCookieId(), json) });

        boolean result = this.manager.isValid(request);
        Assertions.assertFalse(result);
    }

    @Test
    public void test_isValid() {
        Token token = new CsrfToken(300000);
        String cipher = this.manager.toJson(token);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getHeader("X-CSRF")).thenReturn(Hashes.SHA2_256.hash(this.manager.getSalt() + cipher, Encodings.Base64));
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(this.manager.getCookieId(), cipher) });

        boolean result = this.manager.isValid(request);
        Assertions.assertTrue(result);
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
        String badData = Encodings.Base64.encode("bad-json");

        try {
            this.manager.fromJson(badData);
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

    @Test
    public void test_newSalt() {
        final String first1 = this.manager.getSalt();
        final String first2 = this.manager.getSalt();
        this.manager.newSalt();
        final String second1 = this.manager.getSalt();
        final String second2 = this.manager.getSalt();
        this.manager.newSalt();
        final String third1 = this.manager.getSalt();
        final String third2 = this.manager.getSalt();

        Assertions.assertEquals(first1, first2);
        Assertions.assertEquals(second1, second2);
        Assertions.assertEquals(third1, third2);

        Assertions.assertNotEquals(first1, second1);
        Assertions.assertNotEquals(first1, third1);
        Assertions.assertNotEquals(second1, third1);
    }
}
