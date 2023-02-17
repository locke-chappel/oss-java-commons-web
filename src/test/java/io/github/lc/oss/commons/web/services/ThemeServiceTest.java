package io.github.lc.oss.commons.web.services;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.github.lc.oss.commons.testing.AbstractMockTest;
import io.github.lc.oss.commons.web.controllers.ThemeResourceFileResolver;
import io.github.lc.oss.commons.web.controllers.UserTheme;
import io.github.lc.oss.commons.web.util.CookiePrefixParser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ThemeServiceTest extends AbstractMockTest {
    @Mock
    private UserTheme userTheme;

    private CookiePrefixParser cookiePrefixParser = new CookiePrefixParser();

    private ThemeService service;

    @BeforeEach
    public void init() {
        this.service = new ThemeService();
        this.setField("sessionCookieName", "cookie", this.service);
        this.setField("secureCookies", true, this.service);
        this.setField("cookiePath", "/", this.service);
        this.setField("cookieDomain", "localhost", this.service);
        this.setField("cookiePrefixParser", this.cookiePrefixParser, this.service);
        this.setField("cookieName", "theme", this.service);
    }

    @Test
    public void test_getCookieId() {
        String result = this.service.getCookieId();
        Assertions.assertEquals("theme", result);
        Assertions.assertEquals("theme", this.service.getCookieName());

        String result2 = this.service.getCookieId();
        Assertions.assertSame(result, result2);
        Assertions.assertEquals("theme", this.service.getCookieName());

        this.setField("themeCookieId", null, this.service);
        this.setField("sessionCookieName", "__Secure-cookie", this.service);
        this.setField("secureCookies", true, this.service);
        String result3 = this.service.getCookieId();
        Assertions.assertEquals("__Secure-theme", result3);
        Assertions.assertEquals(true, this.getField("secureCookies", this.service));
        Assertions.assertEquals("theme", this.service.getCookieName());

        this.setField("themeCookieId", null, this.service);
        this.setField("sessionCookieName", "__Secure-cookie", this.service);
        this.setField("secureCookies", false, this.service);
        String result4 = this.service.getCookieId();
        Assertions.assertEquals("__Secure-theme", result4);
        Assertions.assertEquals(true, this.getField("secureCookies", this.service));
        Assertions.assertEquals("theme", this.service.getCookieName());

        this.setField("themeCookieId", null, this.service);
        this.setField("sessionCookieName", null, this.service);
        this.setField("secureCookies", false, this.service);
        String result5 = this.service.getCookieId();
        Assertions.assertEquals("theme", result5);
        Assertions.assertEquals("theme", this.service.getCookieName());

        this.setField("themeCookieId", null, this.service);
        this.setField("sessionCookieName", null, this.service);
        this.setField("secureCookies", true, this.service);
        String result6 = this.service.getCookieId();
        Assertions.assertEquals("theme", result6);
        Assertions.assertEquals("theme", this.service.getCookieName());

        this.setField("themeCookieId", null, this.service);
        this.setField("sessionCookieName", "__Host-cookie", this.service);
        this.setField("secureCookies", true, this.service);
        String result7 = this.service.getCookieId();
        Assertions.assertEquals("__Host-theme", result7);
        Assertions.assertEquals(true, this.getField("secureCookies", this.service));
        Assertions.assertEquals("theme", this.service.getCookieName());

        this.setField("themeCookieId", null, this.service);
        this.setField("sessionCookieName", "__Host-cookie", this.service);
        this.setField("secureCookies", false, this.service);
        String result8 = this.service.getCookieId();
        Assertions.assertEquals("__Host-theme", result8);
        Assertions.assertEquals(true, this.getField("secureCookies", this.service));
        Assertions.assertEquals("theme", this.service.getCookieName());
    }

    @Test
    public void test_setThemeCookie() {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie c = invocation.getArgument(0);
                Assertions.assertEquals("theme", c.getValue());
                Assertions.assertEquals(10 * 365 * 24 * 60 * 60, c.getMaxAge());
                return null;
            }
        }).when(response).addCookie(ArgumentMatchers.notNull());

        this.service.setThemeCookie(response, "theme");
    }

    @Test
    public void test_setThemeCookieIfExists_doesntExist_noCookies() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        this.service.setThemeCookieIfExists(request, response, "theme");
    }

    @Test
    public void test_setThemeCookieIfExists_doesntExist_deleteCookie() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie c = invocation.getArgument(0);
                Assertions.assertEquals(ThemeServiceTest.this.service.getCookieId(), c.getName());
                Assertions.assertEquals("theme", c.getValue());
                Assertions.assertEquals(0, c.getMaxAge());
                return null;
            }
        }).when(response).addCookie(ArgumentMatchers.notNull());

        this.service.setThemeCookieIfExists(request, response, "theme");
    }

    @Test
    public void test_setThemeCookieIfExists() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Set<String> validThemes = new HashSet<>();
        validThemes.add("theme");
        this.setField("validThemes", validThemes, this.service);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie c = invocation.getArgument(0);
                Assertions.assertEquals("theme", c.getValue());
                Assertions.assertEquals(10 * 365 * 24 * 60 * 60, c.getMaxAge());
                return null;
            }
        }).when(response).addCookie(ArgumentMatchers.notNull());

        this.service.setThemeCookieIfExists(request, response, "theme");
    }

    @Test
    public void test_themeExists_nullRoot() {
        ThemeResourceFileResolver libThemeResolver = new ThemeResourceFileResolver("library-themes", 1);
        ThemeResourceFileResolver appThemeResolver = new ThemeResourceFileResolver("", 1);
        ThemeResourceFileResolver extThemeResolver = new ThemeResourceFileResolver(null, 1);

        ThemeService service = new ThemeService();
        this.setField("libThemeResolver", libThemeResolver, service);
        this.setField("appThemeResolver", appThemeResolver, service);
        this.setField("extThemeResolver", extThemeResolver, service);

        boolean result = service.themeExists("theme");
        Assertions.assertFalse(result);
    }

    @Test
    public void test_themeExists_blanks() {
        boolean result = this.service.themeExists(null);
        Assertions.assertFalse(result);

        result = this.service.themeExists("");
        Assertions.assertFalse(result);

        result = this.service.themeExists(" \t \r \n \t ");
        Assertions.assertFalse(result);
    }
}
