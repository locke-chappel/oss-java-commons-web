package com.github.lc.oss.commons.web.filters;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import com.github.lc.oss.commons.testing.AbstractMockTest;
import com.github.lc.oss.commons.web.tokens.CsrfTokenManager;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CsrfFilterTest extends AbstractMockTest {
    private static class Helper<T> {
        public T result;
    }

    private HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    private HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    private FilterChain filterChain = Mockito.mock(FilterChain.class);
    private CsrfTokenManager tokenManager = Mockito.mock(CsrfTokenManager.class);

    private CsrfFilter filter;

    @BeforeEach
    public void init() {
        this.filter = new CsrfFilter();
        this.setField("tokenManager", this.tokenManager, this.filter);
    }

    @Test
    public void test_defaultSafeCsrfMethods() {
        Set<String> result = this.filter.getSafeCsrfMethods();
        Assertions.assertSame(CsrfFilter.SAFE_CSRF_METHODS, result);
    }

    @Test
    public void test_requiresToken_apiUrl() {
        Mockito.when(this.request.getRequestURI()).thenReturn("/api/v1/resource");

        boolean result = this.filter.requiresToken(this.request);
        Assertions.assertTrue(result);
    }

    @Test
    public void test_requiresToken_unsafeMethod() {
        Mockito.when(this.request.getRequestURI()).thenReturn("/index");
        Mockito.when(this.request.getMethod()).thenReturn("POST");

        boolean result = this.filter.requiresToken(this.request);
        Assertions.assertTrue(result);
    }

    @Test
    public void test_requiresToken_safeMethod() {
        Mockito.when(this.request.getRequestURI()).thenReturn("/index");
        Mockito.when(this.request.getMethod()).thenReturn("GET");

        boolean result = this.filter.requiresToken(this.request);
        Assertions.assertFalse(result);
    }

    @Test
    public void test_doFilter_noTokenRequired() {
        Mockito.when(this.request.getRequestURI()).thenReturn("/index");
        Mockito.when(this.request.getMethod()).thenReturn("GET");

        try {
            this.filter.doFilterInternal(this.request, this.response, this.filterChain);
        } catch (ServletException | IOException ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_doFilter_validToken() {
        Mockito.when(this.request.getRequestURI()).thenReturn("/api/v1/resource");
        Mockito.when(this.tokenManager.isValid(this.request)).thenReturn(true);

        try {
            this.filter.doFilterInternal(this.request, this.response, this.filterChain);
        } catch (ServletException | IOException ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_doFilter_vinalidToken() {
        Mockito.when(this.request.getRequestURI()).thenReturn("/api/v1/resource");
        Mockito.when(this.tokenManager.isValid(this.request)).thenReturn(false);

        final Helper<HttpStatus> statusHelper = new Helper<>();
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                statusHelper.result = HttpStatus.valueOf((int) invocation.getArgument(0));
                return null;
            }
        }).when(this.response).setStatus(ArgumentMatchers.anyInt());

        final Helper<Boolean> managerHelper = new Helper<>();
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                managerHelper.result = true;
                return null;
            }
        }).when(this.tokenManager).setToken(this.response);

        try {
            this.filter.doFilterInternal(this.request, this.response, this.filterChain);
        } catch (ServletException | IOException ex) {
            Assertions.fail("Unexpected exception");
        }
        Assertions.assertEquals(HttpStatus.FORBIDDEN, statusHelper.result);
        Assertions.assertTrue(managerHelper.result);
    }
}
