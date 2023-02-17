package io.github.lc.oss.commons.web.filters;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.lc.oss.commons.web.controllers.UserTheme;
import io.github.lc.oss.commons.web.services.ThemeService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class UserThemeFilter implements Filter {
    @Autowired
    private ThemeService themeService;
    @Autowired
    private UserTheme userTheme;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            String name = Arrays.stream(cookies). //
                    filter(c -> this.themeService.getCookieId().equals(c.getName())). //
                    map(c -> c.getValue()). //
                    findAny(). //
                    orElse(null);
            if (this.themeService.themeExists(name)) {
                this.userTheme.setName(name.trim());
            }
        }
        chain.doFilter(request, response);
    }

    protected UserTheme getUserTheme() {
        return this.userTheme;
    }
}
