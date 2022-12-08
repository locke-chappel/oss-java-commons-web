package com.github.lc.oss.commons.web.tokens;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.lc.oss.commons.encoding.Encodings;
import com.github.lc.oss.commons.hashing.Hashes;
import com.github.lc.oss.commons.web.util.CookiePrefixParser;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class StatelessCsrfTokenManager implements CsrfTokenManager {
    private static final String CSRF_HEADER_ID = "X-CSRF";
    private static final String CSRF_COOKIE_ID = "csrf";

    private static final int TTL = 5 * 60 * 1000;
    private static final int TTL_COOKIE = StatelessCsrfTokenManager.TTL / 1000;
    protected static final ObjectWriter JSON_WRITER = new ObjectMapper().writer();
    protected static final ObjectReader JSON_READER = new ObjectMapper().readerFor(CsrfToken.class);

    private static String SALT;

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

    private String csrfCookieId;
    private String csrfHeaderCookieId;

    @Override
    public String getHeaderId() {
        return StatelessCsrfTokenManager.CSRF_HEADER_ID;
    }

    @Override
    public String getCookieId() {
        if (this.csrfCookieId == null) {
            String cookieId = StatelessCsrfTokenManager.CSRF_COOKIE_ID;
            if (this.getCookiePrefixParser().isHostCookie(this.sessionCookieName)) {
                cookieId = "__Host-" + cookieId;
                this.secureCookies = true;
            } else if (this.getCookiePrefixParser().isSecureCookie(this.sessionCookieName)) {
                cookieId = "__Secure-" + cookieId;
                this.secureCookies = true;
            }

            this.csrfCookieId = cookieId;
        }
        return this.csrfCookieId;
    }

    @Override
    public String getHeaderCookieId() {
        if (this.csrfHeaderCookieId == null) {
            String cookieId = StatelessCsrfTokenManager.CSRF_HEADER_ID;
            if (this.getCookiePrefixParser().isHostCookie(this.sessionCookieName)) {
                cookieId = "__Host-" + cookieId;
                this.secureCookies = true;
            } else if (this.getCookiePrefixParser().isSecureCookie(this.sessionCookieName)) {
                cookieId = "__Secure-" + cookieId;
                this.secureCookies = true;
            }
            this.csrfHeaderCookieId = cookieId;
        }
        return this.csrfHeaderCookieId;
    }

    protected String getSalt() {
        if (StatelessCsrfTokenManager.SALT == null) {
            this.newSalt();
        }
        return StatelessCsrfTokenManager.SALT;
    }

    public void newSalt() {
        StatelessCsrfTokenManager.SALT = UUID.randomUUID().toString();
    }

    @Override
    public void setToken(HttpServletResponse response) {
        Token token = new CsrfToken(StatelessCsrfTokenManager.TTL);
        String json = this.toJson(token);
        Cookie csrfCookie = new Cookie(this.getCookieId(), json);
        csrfCookie.setPath(this.cookiePath);
        csrfCookie.setDomain(this.cookieDomain);
        csrfCookie.setSecure(this.secureCookies);
        csrfCookie.setHttpOnly(true);
        csrfCookie.setMaxAge(StatelessCsrfTokenManager.TTL_COOKIE);
        response.addCookie(csrfCookie);

        Cookie headerCookie = new Cookie(this.getHeaderCookieId(), Hashes.SHA2_256.hash(this.getSalt() + json, Encodings.Base64));
        headerCookie.setPath(this.cookiePath);
        headerCookie.setDomain(this.cookieDomain);
        headerCookie.setSecure(this.secureCookies);
        headerCookie.setHttpOnly(false);
        headerCookie.setMaxAge(StatelessCsrfTokenManager.TTL_COOKIE);
        response.addCookie(headerCookie);
    }

    @Override
    public boolean isValid(HttpServletRequest request) {
        String header = request.getHeader(StatelessCsrfTokenManager.CSRF_HEADER_ID);
        Cookie cookie = request.getCookies() == null ? null : Arrays.stream(request.getCookies()). //
                filter(c -> this.getCookieId().equals(c.getName())). //
                findAny(). //
                orElse(null);
        if (cookie == null || header == null) {
            return false;
        }

        if (!Hashes.SHA2_256.hash(this.getSalt() + cookie.getValue(), Encodings.Base64).equals(header)) {
            return false;
        }

        Token token = this.fromJson(cookie.getValue());
        if (token == null) {
            return false;
        }

        return !this.isExpired(token);
    }

    private boolean isExpired(Token token) {
        return token.getExpires() <= System.currentTimeMillis();
    }

    protected Token fromJson(String tokenJson) {
        if (tokenJson == null || tokenJson.trim().equals("")) {
            return null;
        }

        try {
            return StatelessCsrfTokenManager.JSON_READER.readValue(Encodings.Base64.decodeString(tokenJson));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Error reading Token JSON.", ex);
        }
    }

    protected String toJson(Token token) {
        if (token == null) {
            return null;
        }

        String json = null;
        try {
            json = Encodings.Base64.encode(StatelessCsrfTokenManager.JSON_WRITER.writeValueAsString(token));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Error writing Token JSON.", ex);
        }
        return json;
    }

    protected CookiePrefixParser getCookiePrefixParser() {
        return this.cookiePrefixParser;
    }
}
