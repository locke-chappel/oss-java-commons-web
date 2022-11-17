package com.github.lc.oss.commons.web.filters;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.lc.oss.commons.web.controllers.UserTheme;
import com.github.lc.oss.commons.web.services.ThemeService;

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
