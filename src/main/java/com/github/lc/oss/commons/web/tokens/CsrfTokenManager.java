package com.github.lc.oss.commons.web.tokens;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface CsrfTokenManager {
    String getCookieId();

    String getHeaderCookieId();

    String getHeaderId();

    void setToken(HttpServletResponse response);

    boolean isValid(HttpServletRequest request);
}
