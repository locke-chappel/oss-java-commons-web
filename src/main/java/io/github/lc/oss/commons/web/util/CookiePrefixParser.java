package io.github.lc.oss.commons.web.util;

public class CookiePrefixParser {
    public boolean isSecureCookie(String cookieName) {
        if (cookieName == null) {
            return false;
        }

        return cookieName.startsWith("__Secure-");
    }

    public boolean isHostCookie(String cookieName) {
        if (cookieName == null) {
            return false;
        }

        return cookieName.startsWith("__Host-");
    }
}
