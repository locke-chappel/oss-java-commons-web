package io.github.lc.oss.commons.web.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import io.github.lc.oss.commons.testing.AbstractMockTest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SecurityHeadersFilterTest extends AbstractMockTest {
    @InjectMocks
    private SecurityHeadersFilter filter;

    @Test
    public void test_doFilterInternal_nullExisting() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        Mockito.when(response.getHeaders(ArgumentMatchers.notNull())).thenReturn(null);

        try {
            this.filter.doFilter(request, response, filterChain);
        } catch (ServletException | IOException e) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_doFilterInternal_existingEmpty() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        Mockito.when(response.getHeaders(ArgumentMatchers.notNull())).thenReturn(new ArrayList<>());

        try {
            this.filter.doFilter(request, response, filterChain);
        } catch (ServletException | IOException e) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_doFilterInternal_hasExisting() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        Mockito.when(response.getHeaders("X-Frame-Options")).thenReturn(Arrays.asList("X-Custom-Value"));

        try {
            this.filter.doFilter(request, response, filterChain);
        } catch (ServletException | IOException e) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_getHeaders_noCors() {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();

        Map<String, String> result = filter.getHeaders();
        Assertions.assertNotNull(result);

        Assertions.assertEquals("default-src 'none'; script-src 'self'; " + //
                "connect-src 'self'; img-src 'self'; style-src 'self'; " + //
                "font-src 'self'; frame-ancestors 'none';", result.get("Content-Security-Policy"));
        Assertions.assertEquals("nosniff", result.get("X-Content-Type-Options"));
        Assertions.assertEquals("deny", result.get("X-Frame-Options"));
        Assertions.assertEquals("0", result.get("X-XSS-Protection"));
    }

    @Test
    public void test_getHeaders_cors_null() {
        SecurityHeadersFilter filter = new SecurityHeadersFilter() {
            @Override
            protected String getCors() {
                return null;
            }
        };

        Map<String, String> result = filter.getHeaders();
        Map<String, String> result2 = filter.getHeaders();
        Assertions.assertNotNull(result);
        Assertions.assertSame(result, result2);

        Assertions.assertEquals("default-src 'none'; script-src 'self'; " + //
                "connect-src 'self'; img-src 'self'; style-src 'self'; " + //
                "font-src 'self'; frame-ancestors 'none';", result.get("Content-Security-Policy"));
        Assertions.assertEquals("nosniff", result.get("X-Content-Type-Options"));
        Assertions.assertEquals("deny", result.get("X-Frame-Options"));
        Assertions.assertEquals("0", result.get("X-XSS-Protection"));
    }

    @Test
    public void test_getHeaders_cors_blank() {
        SecurityHeadersFilter filter = new SecurityHeadersFilter() {
            @Override
            protected String getCors() {
                return " \t \r \n \t ";
            }
        };

        Map<String, String> result = filter.getHeaders();
        Assertions.assertNotNull(result);

        Assertions.assertEquals("default-src 'none'; script-src 'self'; " + //
                "connect-src 'self'; img-src 'self'; style-src 'self'; " + //
                "font-src 'self'; frame-ancestors 'none';", result.get("Content-Security-Policy"));
        Assertions.assertEquals("nosniff", result.get("X-Content-Type-Options"));
        Assertions.assertEquals("deny", result.get("X-Frame-Options"));
        Assertions.assertEquals("0", result.get("X-XSS-Protection"));
    }

    @Test
    public void test_getHeaders_cors_site() {
        SecurityHeadersFilter filter = new SecurityHeadersFilter() {
            @Override
            protected String getCors() {
                return " http://localhost:8080 ";
            }
        };

        Map<String, String> result = filter.getHeaders();
        Assertions.assertNotNull(result);

        Assertions.assertEquals("default-src 'none'; script-src 'self'; " + //
                "connect-src 'self' http://localhost:8080; img-src 'self'; style-src 'self'; " + //
                "font-src 'self'; frame-ancestors 'none';", result.get("Content-Security-Policy"));
        Assertions.assertEquals("nosniff", result.get("X-Content-Type-Options"));
        Assertions.assertEquals("deny", result.get("X-Frame-Options"));
        Assertions.assertEquals("0", result.get("X-XSS-Protection"));
    }
}
