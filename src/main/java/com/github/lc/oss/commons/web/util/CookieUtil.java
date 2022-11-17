package com.github.lc.oss.commons.web.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookieUtil {
    private static final Set<String> EMPTY_SET = Collections.unmodifiableSet(new HashSet<>());

    private CookieUtil() {
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String cookieName) {
        if (request == null || request.getCookies() == null || response == null) {
            return;
        }

        Arrays.stream(request.getCookies()). //
                filter(c -> c.getName().equals(cookieName)). //
                forEach(c ->

                {
                    c.setMaxAge(0);
                    response.addCookie(c);
                });
    }

    public static void deleteCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookies(request, response, null);
    }

    public static void deleteCookies(HttpServletRequest request, HttpServletResponse response, Set<String> retain) {
        if (request == null || request.getCookies() == null || response == null) {
            return;
        }

        Set<String> toKeep = retain == null ? CookieUtil.EMPTY_SET : retain;
        Arrays.stream(request.getCookies()). //
                filter(c -> !toKeep.contains(c.getName())). //
                forEach(c -> {
                    c.setMaxAge(0);
                    response.addCookie(c);
                });
    }

    public static Cookie getCookie(HttpServletRequest request, String cookieName) {
        if (request == null || request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies()) //
                .filter(c -> c.getName().equals(cookieName)) //
                .findAny() //
                .orElse(null);
    }
}
