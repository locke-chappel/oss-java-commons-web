package io.github.lc.oss.commons.web.filters;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.l10n.UserLocale;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

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
                        map(l -> Locale.forLanguageTag(l.getRange())). //
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
