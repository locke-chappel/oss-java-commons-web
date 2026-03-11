package io.github.lc.oss.commons.web.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.lc.oss.commons.encryption.config.ConfigKey;
import io.github.lc.oss.commons.encryption.config.EncryptedConfig;
import io.github.lc.oss.commons.serialization.TrimmingModule;
import io.github.lc.oss.commons.util.PathNormalizer;
import io.github.lc.oss.commons.web.controllers.ExceptionController;
import io.github.lc.oss.commons.web.filters.SecurityHeadersFilter;
import io.github.lc.oss.commons.web.util.ConfigLoader;

public abstract class AbstractConfiguration implements WebMvcConfigurer {
    @Value("${application.ephemeral-ciphers.keyfile:${user.home}/ephemeral/key}")
    private String ephemeralKeyFile;
    @Value("${application.ephemeral-ciphers.configfile:${user.home}/ephemeral/config}")
    private String ephemeralConfigFile;
    @Value("${application.ephemeral-ciphers.timeout:300}")
    private int ephemeralTimeout;

    protected boolean isIntegrationtest(Environment env) {
        return env.getProperty("integrationtest", Boolean.class, Boolean.FALSE);
    }

    protected boolean isKnative(Environment env) {
        return env.getProperty("knative", Boolean.class, Boolean.FALSE);
    }

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

    protected <T extends EncryptedConfig, K extends ConfigKey> T loadEncryptedConfig(@Autowired Environment env,
            K[] keys, Class<T> clazz) {
        if (this.isKnative(env)) {
            return ConfigLoader.loadJsonFromEnv(env, "CONFIG", clazz);
        } else if (this.isIntegrationtest(env)) {
            return ConfigLoader.loadFromProperties(env, keys, clazz);
        } else {
            return ConfigLoader.loadFromFile(this.ephemeralKeyFile, this.ephemeralConfigFile,
                    this.ephemeralTimeout, clazz);
        }
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
        http.authorizeHttpRequests((ahr) -> ahr //
                .anyRequest() //
                .denyAll());

        this.configureDefaultHeaders(http);
    }

    protected void configureDefaultPublicAccessUrls(HttpSecurity http) throws Exception {
        /*
         * Nothing is public by default
         */
    }

    protected void configureDefaultHeaders(HttpSecurity http) throws Exception {
        /* Spring's CSRF protection is not RESTful */
        http.csrf((csrf) -> csrf.disable());

        /* Disable auto-creation of sessions */
        http.sessionManagement((sm) -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

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
