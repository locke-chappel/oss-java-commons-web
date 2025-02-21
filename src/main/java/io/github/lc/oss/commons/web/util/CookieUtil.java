package io.github.lc.oss.commons.web.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {
    private static final Set<String> EMPTY_SET = Collections.unmodifiableSet(new HashSet<>());

    private CookieUtil() {
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String cookieName) {
        CookieUtil.deleteCookies(request, response, cookieName);
    }

    public static void deleteCookies(HttpServletRequest request, HttpServletResponse response, String... toDelete) {
        Set<String> set = CookieUtil.isNullOrEmpty(toDelete) ? CookieUtil.EMPTY_SET : Set.of(toDelete);
        CookieUtil.deleteCookies(request, response, set);
    }

    public static void deleteCookies(HttpServletRequest request, HttpServletResponse response, Set<String> toDelete) {
        if (CookieUtil.isNullOrEmpty(request) || CookieUtil.isNullOrEmpty(request.getCookies())
                || CookieUtil.isNullOrEmpty(response) || CookieUtil.isNullOrEmpty(toDelete)) {
            return;
        }

        Arrays.stream(request.getCookies()). //
                filter(c -> toDelete.contains(c.getName())). //
                forEach(c -> {
                    c.setMaxAge(0);
                    response.addCookie(c);
                });
    }

    public static void deleteAllCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteAllCookies(request, response, (Set<String>) null);
    }

    public static void deleteAllCookies(HttpServletRequest request, HttpServletResponse response, String... except) {
        Set<String> set = CookieUtil.isNullOrEmpty(except) ? CookieUtil.EMPTY_SET : Set.of(except);
        CookieUtil.deleteAllCookies(request, response, set);
    }

    public static void deleteAllCookies(HttpServletRequest request, HttpServletResponse response, Set<String> except) {
        if (CookieUtil.isNullOrEmpty(request) || CookieUtil.isNullOrEmpty(request.getCookies())
                || CookieUtil.isNullOrEmpty(response)) {
            return;
        }

        Set<String> toKeep = except == null ? CookieUtil.EMPTY_SET : except;
        Arrays.stream(request.getCookies()). //
                filter(c -> !toKeep.contains(c.getName())). //
                forEach(c -> {
                    c.setMaxAge(0);
                    response.addCookie(c);
                });
    }

    public static Cookie getCookie(HttpServletRequest request, String cookieName) {
        if (CookieUtil.isNullOrEmpty(request) || CookieUtil.isNullOrEmpty(request.getCookies())) {
            return null;
        }

        return Arrays.stream(request.getCookies()) //
                .filter(c -> c.getName().equals(cookieName)) //
                .findAny() //
                .orElse(null);
    }

    private static boolean isNullOrEmpty(Object o) {
        if (o == null) {
            return true;
        } else if (o instanceof Collection) {
            return ((Collection<?>) o).isEmpty();
        } else if (o.getClass().isArray()) {
            if (Array.getLength(o) < 1) {
                // Empty array
                return true;
            }
            if (Stream.of((Object[]) o).allMatch(i -> i == null)) {
                // array is all nulls - happens if vararg was passed a null of matching type
                return true;
            }
        }
        return false;
    }
}
