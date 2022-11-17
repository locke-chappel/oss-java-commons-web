package com.github.lc.oss.commons.web.filters;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.lc.oss.commons.web.tokens.CsrfTokenManager;

public class CsrfFilter extends OncePerRequestFilter {
    protected static final Set<String> SAFE_CSRF_METHODS;
    static {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.add(HttpMethod.GET.name());
        set.add(HttpMethod.HEAD.name());
        set.add(HttpMethod.OPTIONS.name());
        SAFE_CSRF_METHODS = Collections.unmodifiableSet(set);
    }

    @Autowired
    private CsrfTokenManager tokenManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean requiresToken = this.requiresToken(request);
        if (!requiresToken) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isValid = this.tokenManager.isValid(request);
        if (!isValid) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            this.tokenManager.setToken(response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    protected boolean requiresToken(HttpServletRequest request) {
        String url = request.getRequestURI();
        if (this.isApiUrl(url)) {
            return true;
        }

        return !this.getSafeCsrfMethods().contains(request.getMethod());
    }

    protected Set<String> getSafeCsrfMethods() {
        return CsrfFilter.SAFE_CSRF_METHODS;
    }

    protected boolean isApiUrl(String url) {
        return url.toLowerCase().startsWith("/api");
    }
}
