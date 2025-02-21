package io.github.lc.oss.commons.web.util;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.github.lc.oss.commons.testing.AbstractMockTest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtilTest extends AbstractMockTest {
    private static class Helper {
        public boolean wasCalled = false;
    }

    private HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    private HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    @Test
    public void test_deleteCookie_nulls() {
        Mockito.when(this.request.getCookies()).thenReturn(null);

        CookieUtil.deleteCookie(null, null, null);
        CookieUtil.deleteCookie(this.request, null, null);
        CookieUtil.deleteCookie(this.request, this.response, null);
    }

    @Test
    public void test_deleteCookie_nulls_v2() {
        Mockito.when(this.request.getCookies()).thenReturn(new Cookie[] {});

        CookieUtil.deleteCookie(this.request, null, null);
        CookieUtil.deleteCookie(this.request, this.response, null);
    }

    @Test
    public void test_deleteCookie_nulls_v3() {
        Mockito.when(this.request.getCookies()).thenReturn(new Cookie[] { new Cookie("junit-other", "junit-value") });

        CookieUtil.deleteCookie(this.request, null, null);
        CookieUtil.deleteCookie(this.request, this.response, null);
    }

    @Test
    public void test_deleteCookie_noCookies() {
        Cookie[] cookies = new Cookie[] {};

        Mockito.when(this.request.getCookies()).thenReturn(cookies);
        Mockito.verify(this.response, Mockito.never()).addCookie(ArgumentMatchers.any());

        CookieUtil.deleteCookie(this.request, this.response, "junit-cookie");
    }

    @Test
    public void test_deleteCookie_noMatch() {
        Cookie[] cookies = new Cookie[] { new Cookie("junit-other", "junit-value") };

        Mockito.when(this.request.getCookies()).thenReturn(cookies);
        Mockito.verify(this.response, Mockito.never()).addCookie(ArgumentMatchers.any());

        CookieUtil.deleteCookie(this.request, this.response, "junit-cookie");
    }

    @Test
    public void test_deleteCookie_found() {
        Cookie[] cookies = new Cookie[] { new Cookie("junit-other", "junit-value"),
                new Cookie("junit-cookie", "value-junit") };

        final Helper addCookie = new Helper();

        Mockito.when(this.request.getCookies()).thenReturn(cookies);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Assertions.assertFalse(addCookie.wasCalled);
                addCookie.wasCalled = true;
                Assertions.assertEquals(0, ((Cookie) invocation.getArgument(0)).getMaxAge());
                return null;
            }
        }).when(this.response).addCookie(ArgumentMatchers.notNull());

        Assertions.assertFalse(addCookie.wasCalled);
        CookieUtil.deleteCookie(this.request, this.response, "junit-cookie");
        Assertions.assertTrue(addCookie.wasCalled);
    }

    @Test
    public void test_deleteAllCookies_nulls() {
        Mockito.when(this.request.getCookies()).thenReturn(null);

        CookieUtil.deleteAllCookies(null, null, (Set<String>) null);
        CookieUtil.deleteAllCookies(this.request, null, (Set<String>) null);
        CookieUtil.deleteAllCookies(this.request, this.response, (Set<String>) null);
    }

    @Test
    public void test_deleteAllCookies_nulls_v2() {
        Mockito.when(this.request.getCookies()).thenReturn(new Cookie[] {});

        CookieUtil.deleteAllCookies(this.request, null, (Set<String>) null);
        CookieUtil.deleteAllCookies(this.request, this.response, (Set<String>) null);
    }

    @Test
    public void test_deleteAllCookies_nulls_v3() {
        Mockito.when(this.request.getCookies()).thenReturn(new Cookie[] { new Cookie("junit-cookie", "value-junit") });

        CookieUtil.deleteAllCookies(this.request, null, (Set<String>) null);
        CookieUtil.deleteAllCookies(this.request, this.response, (Set<String>) null);
    }

    @Test
    public void test_deleteAllCookies_noCookies() {
        Cookie[] cookies = new Cookie[] {};

        Mockito.when(this.request.getCookies()).thenReturn(cookies);
        Mockito.verify(this.response, Mockito.never()).addCookie(ArgumentMatchers.any());

        CookieUtil.deleteAllCookies(this.request, this.response);
    }

    @Test
    public void test_deleteAllCookies() {
        Cookie[] cookies = new Cookie[] { new Cookie("junit-other", "junit-value"),
                new Cookie("junit-cookie", "value-junit") };

        final Helper otherDeleted = new Helper();
        final Helper cookieDeleted = new Helper();

        Mockito.when(this.request.getCookies()).thenReturn(cookies);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie cookie = (Cookie) invocation.getArgument(0);
                switch (cookie.getName()) {
                    case "junit-other":
                        Assertions.assertFalse(otherDeleted.wasCalled);
                        otherDeleted.wasCalled = true;
                        break;
                    case "junit-cookie":
                        Assertions.assertFalse(cookieDeleted.wasCalled);
                        cookieDeleted.wasCalled = true;
                        break;
                    default:
                        Assertions.fail("Unexpected cookie: " + cookie.getName());
                }
                Assertions.assertEquals(0, cookie.getMaxAge());
                return null;
            }
        }).when(this.response).addCookie(ArgumentMatchers.notNull());

        Assertions.assertFalse(otherDeleted.wasCalled);
        Assertions.assertFalse(cookieDeleted.wasCalled);

        CookieUtil.deleteAllCookies(this.request, this.response, (Set<String>) null);

        Assertions.assertTrue(otherDeleted.wasCalled);
        Assertions.assertTrue(cookieDeleted.wasCalled);
    }

    @Test
    public void test_deleteAllCookies_v2() {
        Cookie[] cookies = new Cookie[] { new Cookie("junit-other", "junit-value"),
                new Cookie("junit-cookie", "value-junit") };

        final Helper otherDeleted = new Helper();
        final Helper cookieDeleted = new Helper();

        Mockito.when(this.request.getCookies()).thenReturn(cookies);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie cookie = (Cookie) invocation.getArgument(0);
                switch (cookie.getName()) {
                    case "junit-other":
                        Assertions.assertFalse(otherDeleted.wasCalled);
                        otherDeleted.wasCalled = true;
                        break;
                    case "junit-cookie":
                        Assertions.assertFalse(cookieDeleted.wasCalled);
                        cookieDeleted.wasCalled = true;
                        break;
                    default:
                        Assertions.fail("Unexpected cookie: " + cookie.getName());
                }
                Assertions.assertEquals(0, cookie.getMaxAge());
                return null;
            }
        }).when(this.response).addCookie(ArgumentMatchers.notNull());

        Assertions.assertFalse(otherDeleted.wasCalled);
        Assertions.assertFalse(cookieDeleted.wasCalled);

        CookieUtil.deleteAllCookies(this.request, this.response, (String) null);

        Assertions.assertTrue(otherDeleted.wasCalled);
        Assertions.assertTrue(cookieDeleted.wasCalled);
    }

    @Test
    public void test_deleteAllCookies_v3() {
        Cookie[] cookies = new Cookie[] { new Cookie("junit-other", "junit-value"),
                new Cookie("junit-cookie", "value-junit") };

        final Helper otherDeleted = new Helper();
        final Helper cookieDeleted = new Helper();

        Mockito.when(this.request.getCookies()).thenReturn(cookies);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie cookie = (Cookie) invocation.getArgument(0);
                switch (cookie.getName()) {
                    case "junit-other":
                        Assertions.assertFalse(otherDeleted.wasCalled);
                        otherDeleted.wasCalled = true;
                        break;
                    case "junit-cookie":
                        Assertions.assertFalse(cookieDeleted.wasCalled);
                        cookieDeleted.wasCalled = true;
                        break;
                    default:
                        Assertions.fail("Unexpected cookie: " + cookie.getName());
                }
                Assertions.assertEquals(0, cookie.getMaxAge());
                return null;
            }
        }).when(this.response).addCookie(ArgumentMatchers.notNull());

        Assertions.assertFalse(otherDeleted.wasCalled);
        Assertions.assertFalse(cookieDeleted.wasCalled);

        CookieUtil.deleteAllCookies(this.request, this.response);

        Assertions.assertTrue(otherDeleted.wasCalled);
        Assertions.assertTrue(cookieDeleted.wasCalled);
    }

    @Test
    public void test_deleteAllCookies_exceptOne() {
        Cookie[] cookies = new Cookie[] { new Cookie("junit-other", "junit-value"),
                new Cookie("junit-cookie", "value-junit") };

        final Helper otherDeleted = new Helper();

        Mockito.when(this.request.getCookies()).thenReturn(cookies);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie cookie = (Cookie) invocation.getArgument(0);
                Assertions.assertEquals("junit-other", cookie.getName());
                Assertions.assertEquals(0, cookie.getMaxAge());
                otherDeleted.wasCalled = true;
                return null;
            }
        }).when(this.response).addCookie(ArgumentMatchers.notNull());

        Assertions.assertFalse(otherDeleted.wasCalled);

        CookieUtil.deleteAllCookies(this.request, this.response, "junit-cookie");

        Assertions.assertTrue(otherDeleted.wasCalled);
    }

    @Test
    public void test_getCookie_nulls() {
        Mockito.when(this.request.getCookies()).thenReturn(null);

        Cookie result = CookieUtil.getCookie(null, null);
        Assertions.assertNull(result);

        result = CookieUtil.getCookie(this.request, null);
        Assertions.assertNull(result);
    }

    @Test
    public void test_getCookie_notFound() {
        Mockito.when(this.request.getCookies()).thenReturn(new Cookie[] {});

        Cookie result = CookieUtil.getCookie(this.request, "junit-cookie");
        Assertions.assertNull(result);
    }

    @Test
    public void test_getCookie() {
        Cookie match = new Cookie("junit-cookie", "value-junit");
        Cookie[] cookies = new Cookie[] { new Cookie("junit-other", "junit-value"), match };

        Mockito.when(this.request.getCookies()).thenReturn(cookies);

        Cookie result = CookieUtil.getCookie(this.request, "junit-cookie");
        Assertions.assertSame(match, result);
    }
}
