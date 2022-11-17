package com.github.lc.oss.commons.web.util;

import javax.servlet.ServletContext;

public class ContextUtil {
    public static String getAbsoluteUrl(String path, ServletContext context) {
        if (context == null) {
            throw new NullPointerException("context is required");
        }

        String url = context.getContextPath();
        if (url == null) {
            url = "/";
        }
        url = url.trim();
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        if (!url.endsWith("/")) {
            url += "/";
        }

        String subpath = path;
        if (subpath == null) {
            return url;
        }

        subpath = subpath.trim();
        if (subpath.startsWith("/")) {
            subpath = subpath.substring(1);
        }

        return url + subpath;
    }

    private ContextUtil() {
    }
}
