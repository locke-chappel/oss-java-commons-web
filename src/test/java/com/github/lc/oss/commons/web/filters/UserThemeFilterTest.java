package com.github.lc.oss.commons.web.filters;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.github.lc.oss.commons.testing.AbstractMockTest;
import com.github.lc.oss.commons.web.controllers.UserTheme;
import com.github.lc.oss.commons.web.services.ThemeService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UserThemeFilterTest extends AbstractMockTest {
    @Mock
    private ThemeService themeService;

    @InjectMocks
    private UserThemeFilter filter;

    @BeforeEach
    public void init() {
        UserTheme userTheme = new UserTheme();
        this.setField("userTheme", userTheme, this.filter);
    }

    @Test
    public void test_noCookie() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        try {
            this.filter.doFilter(request, response, chain);
        } catch (IOException | ServletException ex) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertNull(this.filter.getUserTheme().getName());
    }

    @Test
    public void test_badTheme() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(this.themeService.getCookieId()).thenReturn("theme");
        Mockito.when(this.themeService.themeExists("doesnt-exist")).thenReturn(false);

        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie("theme", "doesnt-exist");
        Mockito.when(request.getCookies()).thenReturn(cookies);

        try {
            this.filter.doFilter(request, response, chain);
        } catch (IOException | ServletException ex) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertNull(this.filter.getUserTheme().getName());
    }

    @Test
    public void test_hasTheme() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(this.themeService.getCookieId()).thenReturn("theme");
        Mockito.when(this.themeService.themeExists("name")).thenReturn(true);

        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie("theme", "name");
        Mockito.when(request.getCookies()).thenReturn(cookies);

        try {
            this.filter.doFilter(request, response, chain);
        } catch (IOException | ServletException ex) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertEquals("name", this.filter.getUserTheme().getName());
    }
}
