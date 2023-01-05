package com.github.lc.oss.commons.web.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.lc.oss.commons.serialization.TrimmingModule;
import com.github.lc.oss.commons.util.PathNormalizer;
import com.github.lc.oss.commons.web.controllers.ExceptionController;
import com.github.lc.oss.commons.web.filters.SecurityHeadersFilter;

public abstract class AbstractConfiguration implements WebMvcConfigurer {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public ExceptionController exceptionController() {
        return new ExceptionController();
    }

    @Bean
    public PathNormalizer pathNormalizer() {
        return new PathNormalizer();
    }

    @Bean
    public TrimmingModule trimmingModule() {
        return new TrimmingModule();
    }

    /* Security */
    protected RegexRequestMatcher[] matchers(HttpMethod method, String... patterns) {
        RegexRequestMatcher[] matchers = new RegexRequestMatcher[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            matchers[i] = RegexRequestMatcher.regexMatcher(method, patterns[i]);
        }
        return matchers;
    }

    protected void configureDefaults(HttpSecurity http) throws Exception {
        this.configureDefaultPublicAccessUrls(http);

        /* Default deny all rule */
        http.authorizeHttpRequests(). //
                anyRequest(). //
                denyAll();

        this.configureDefaultHeaders(http);
    }

    protected void configureDefaultPublicAccessUrls(HttpSecurity http) throws Exception {
        /*
         * Nothing is public by default
         */
    }

    protected void configureDefaultHeaders(HttpSecurity http) throws Exception {
        /* Spring's CSRF protection is not RESTful */
        http.csrf().disable();

        /* Disable auto-creation of sessions */
        http.sessionManagement(). //
                sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        /* Add standard security headers */
        http.addFilterAfter(this.securityHeadersFilter(), BasicAuthenticationFilter.class);
    }

    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}
