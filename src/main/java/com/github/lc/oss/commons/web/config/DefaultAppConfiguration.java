package com.github.lc.oss.commons.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.context.annotation.RequestScope;

import com.github.lc.oss.commons.l10n.L10N;
import com.github.lc.oss.commons.l10n.UserLocale;
import com.github.lc.oss.commons.web.advice.CommonAdvice;
import com.github.lc.oss.commons.web.advice.ETagAdvice;
import com.github.lc.oss.commons.web.controllers.ThemeResourceFileResolver;
import com.github.lc.oss.commons.web.controllers.UserTheme;
import com.github.lc.oss.commons.web.filters.CsrfFilter;
import com.github.lc.oss.commons.web.filters.UserLocaleFilter;
import com.github.lc.oss.commons.web.filters.UserThemeFilter;
import com.github.lc.oss.commons.web.resources.Minifier;
import com.github.lc.oss.commons.web.resources.MinifierService;
import com.github.lc.oss.commons.web.services.ThemeService;
import com.github.lc.oss.commons.web.tokens.CsrfTokenManager;
import com.github.lc.oss.commons.web.tokens.StatelessCsrfTokenManager;
import com.github.lc.oss.commons.web.util.CookiePrefixParser;

public class DefaultAppConfiguration extends AbstractConfiguration {
    @Value("${application.security.enableCsrfProtection:true}")
    private boolean enableCsrfProtection;

    @Bean
    public CommonAdvice commonAdvice() {
        return new CommonAdvice();
    }

    @Bean
    public CookiePrefixParser cookiePrefixParser() {
        return new CookiePrefixParser();
    }

    @Bean
    public ETagAdvice eTagAdvice() {
        return new ETagAdvice();
    }

    @Bean
    public L10N l10n() {
        return new L10N() {
            @Value("${application.ui.caching:true}")
            private boolean caching;
            @Value("${application.l10n.external-path:}")
            private String externalL10NRoot;

            @Override
            protected String getExternalL10NRoot() {
                return this.externalL10NRoot;
            }

            @Override
            public boolean isCaching() {
                return this.caching;
            }
        };
    }

    @Bean
    public Minifier minifier(@Value("${application.services.minifier.enabled:true}") boolean enabled) {
        MinifierService service = new MinifierService();
        service.setEnabled(enabled);
        return service;
    }

    @Bean
    @RequestScope
    public UserLocale userLocale() {
        return new UserLocale();
    }

    @Bean
    public UserLocaleFilter userLocaleFilter() {
        return new UserLocaleFilter();
    }

    @Bean
    public ThemeService themeService() {
        return new ThemeService();
    }

    @Bean
    @RequestScope
    public UserTheme userTheme() {
        return new UserTheme();
    }

    @Bean("libThemeResourceFileResolver")
    public ThemeResourceFileResolver libThemeResourceFileResolver() {
        return new ThemeResourceFileResolver("library-themes/", 3);
    }

    @Bean("appThemeResourceFileResolver")
    public ThemeResourceFileResolver appThemeResourceFileResolver(//
            @Value("#{pathNormalizer.dir('${application.ui.resource-theme-path:static-themes/}')}") String appThemePath, //
            @Value("${application.ui.research.search-depth:5}") int searchDepth) {
        return new ThemeResourceFileResolver(appThemePath, searchDepth);
    }

    @Bean("extThemeResourceFileResolver")
    public ThemeResourceFileResolver extThemeResourceFileResolver(//
            @Value("#{pathNormalizer.dir('${application.ui.external-theme-path:}')}") String extThemePath, //
            @Value("${application.ui.research.search-depth:5}") int searchDepth) {
        return new ThemeResourceFileResolver(extThemePath, searchDepth);
    }

    @Bean
    public UserThemeFilter userThemeFilter() {
        return new UserThemeFilter();
    }

    /* Security */
    @Override
    protected void configureDefaultPublicAccessUrls(HttpSecurity http) throws Exception {
        /* Public Access */
        http.authorizeHttpRequests(). //
                requestMatchers(this.matchers(HttpMethod.GET, //
                        /* Resources */
                        "^/css$", //
                        "^/favicon.ico$", //
                        "^/font/fontawesome/fa-(?:brands|regular|solid)-(?:400|900).woff2$", //
                        "^/js$", //
                        "^/js/[a-zA-Z0-9]+$", //
                        "^/l10n/[a-z]{2}(?:-[A-Z]{2})?/messages.Application.Error.1$"))
                .permitAll();
    }

    @Override
    protected void configureDefaultHeaders(HttpSecurity http) throws Exception {
        super.configureDefaultHeaders(http);

        /* Add RESTful CSRF protection solution */
        if (this.enableCsrfProtection) {
            http.addFilterAfter(this.csrfFilter(), BasicAuthenticationFilter.class);
        }
    }

    @Bean
    @Conditional(EnableCsrfProtection.class)
    public CsrfFilter csrfFilter() {
        return new CsrfFilter();
    }

    @Bean
    public CsrfTokenManager csrfTokenManager() {
        return new StatelessCsrfTokenManager();
    }
}
