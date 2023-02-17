package io.github.lc.oss.commons.web.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import io.github.lc.oss.commons.util.IoTools;
import io.github.lc.oss.commons.web.controllers.ThemeResourceFileResolver;
import io.github.lc.oss.commons.web.util.CookiePrefixParser;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ThemeService {
    private static final int COOKIE_MAX_AGE = 10 * 365 * 24 * 60 * 60;

    @Autowired(required = false)
    @Qualifier("libThemeResourceFileResolver")
    private ThemeResourceFileResolver libThemeResolver;
    @Autowired(required = false)
    @Qualifier("appThemeResourceFileResolver")
    private ThemeResourceFileResolver appThemeResolver;
    @Autowired(required = false)
    @Qualifier("extThemeResourceFileResolver")
    private ThemeResourceFileResolver extThemeResolver;
    @Autowired
    private CookiePrefixParser cookiePrefixParser;

    @Value("${server.servlet.session.cookie.name:}")
    private String sessionCookieName;
    @Value("${server.servlet.session.cookie.secure:true}")
    private boolean secureCookies;
    @Value("${server.servlet.session.cookie.path:}")
    private String cookiePath;
    @Value("${server.servlet.session.cookie.domain:}")
    private String cookieDomain;

    @Value("${application.themes.cookie-name:theme}")
    private String cookieName;

    private String themeCookieId;

    private Set<String> validThemes;

    public String getCookieId() {
        if (this.themeCookieId == null) {
            String cookieId = this.cookieName;
            if (this.getCookiePrefixParser().isHostCookie(this.sessionCookieName)) {
                cookieId = "__Host-" + cookieId;
                this.secureCookies = true;
            } else if (this.getCookiePrefixParser().isSecureCookie(this.sessionCookieName)) {
                cookieId = "__Secure-" + cookieId;
                this.secureCookies = true;
            }

            this.themeCookieId = cookieId;
        }
        return this.themeCookieId;
    }

    protected String getCookieName() {
        return this.cookieName;
    }

    public void setThemeCookieIfExists(HttpServletRequest request, HttpServletResponse response, String theme) {
        if (this.themeExists(theme)) {
            this.setThemeCookie(response, theme);
        } else {
            this.setThemeCookie(response, theme, 0);
        }
    }

    public void setThemeCookie(HttpServletResponse response, String theme) {
        this.setThemeCookie(response, theme, ThemeService.COOKIE_MAX_AGE);
    }

    protected void setThemeCookie(HttpServletResponse response, String theme, int maxAge) {
        Cookie cookie = new Cookie(this.getCookieId(), theme);
        cookie.setPath(this.cookiePath);
        cookie.setDomain(this.cookieDomain);
        cookie.setSecure(this.secureCookies);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public boolean themeExists(String name) {
        if (name == null || name.trim().equals("")) {
            return false;
        }

        if (this.validThemes == null) {
            Set<String> themes = new HashSet<>();
            themes.addAll(this.getThemes(this.getLibThemeResolver()));
            themes.addAll(this.getThemes(this.getAppThemeResolver()));
            themes.addAll(this.getThemes(this.getExtThemeResolver()));
            this.validThemes = Collections.unmodifiableSet(themes);
        }
        return this.validThemes.contains(name);
    }

    private List<String> getThemes(ThemeResourceFileResolver resolver) {
        if (resolver == null || resolver.getThemesRoot() == null) {
            return new ArrayList<>();
        }
        return IoTools.listDir(resolver.getThemesRoot(), 1). //
                stream(). //
                map(p -> p.replace(IoTools.getAbsoluteFilePath(resolver.getThemesRoot().substring(0, resolver.getThemesRoot().length() - 1)), "")). //
                map(p -> p.replace("\\", "/")). //
                map(p -> {
                    if (p.startsWith("/")) {
                        return p.substring(1);
                    }
                    return p;
                }). //
                filter(p -> !p.equals("")). //
                collect(Collectors.toList());
    }

    protected ThemeResourceFileResolver getLibThemeResolver() {
        return this.libThemeResolver;
    }

    protected ThemeResourceFileResolver getAppThemeResolver() {
        return this.appThemeResolver;
    }

    protected ThemeResourceFileResolver getExtThemeResolver() {
        return this.extThemeResolver;
    }

    protected CookiePrefixParser getCookiePrefixParser() {
        return this.cookiePrefixParser;
    }
}
