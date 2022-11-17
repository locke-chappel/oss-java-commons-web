package com.github.lc.oss.commons.web.tokens;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface CsrfTokenManager {
    String getCookieId();

    String getHeaderCookieId();

    String getHeaderId();

    void setToken(HttpServletResponse response);

    boolean isValid(HttpServletRequest request);
}
