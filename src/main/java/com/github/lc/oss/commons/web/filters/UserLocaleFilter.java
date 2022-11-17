package com.github.lc.oss.commons.web.filters;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import com.github.lc.oss.commons.l10n.L10N;
import com.github.lc.oss.commons.l10n.UserLocale;

public class UserLocaleFilter implements Filter {
    @Autowired
    private L10N l10n;
    @Autowired
    private UserLocale userLocale;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            Locale locale = this.getL10n().getDefaultLocale();
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String header = httpRequest.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
            if (header != null && !header.trim().equals("")) {
                List<Locale.LanguageRange> acceptedLanguages = Locale.LanguageRange.parse(header);
                Locale match = acceptedLanguages.stream(). //
                        map(l -> new Locale(l.getRange())). //
                        filter(l -> this.getL10n().hasLocale(l)). //
                        findFirst(). //
                        orElse(null);
                if (match != null) {
                    locale = match;
                }
            }
            this.getUserLocale().setLocale(locale);
        } catch (Throwable ex) {
            this.getUserLocale().setLocale(this.getL10n().getDefaultLocale());
        }

        chain.doFilter(request, response);
    }

    protected L10N getL10n() {
        return this.l10n;
    }

    protected UserLocale getUserLocale() {
        return this.userLocale;
    }
}
