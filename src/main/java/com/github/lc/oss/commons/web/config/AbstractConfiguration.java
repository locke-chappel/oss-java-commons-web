package com.github.lc.oss.commons.web.config;

import java.nio.file.Files;
import java.nio.file.Paths;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.lc.oss.commons.encoding.Encodings;
import com.github.lc.oss.commons.encryption.config.ConfigKey;
import com.github.lc.oss.commons.encryption.config.EncryptedConfig;
import com.github.lc.oss.commons.encryption.config.EncryptedConfigUtil;
import com.github.lc.oss.commons.serialization.TrimmingModule;
import com.github.lc.oss.commons.util.PathNormalizer;
import com.github.lc.oss.commons.web.controllers.ExceptionController;
import com.github.lc.oss.commons.web.filters.SecurityHeadersFilter;
import com.github.lc.oss.commons.web.util.PropertiesConfigUtil;

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

    protected <T extends EncryptedConfig, K extends ConfigKey> T loadEncryptedConfig(@Autowired Environment env, K[] keys, Class<T> clazz) {
        if (this.isKnative(env)) {
            try {
                String json = env.getProperty("CONFIG", "");
                if (json.charAt(0) != '{') {
                    json = Encodings.Base64.decodeString(json);
                }

                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json, clazz);
            } catch (Exception ex) {
                throw new RuntimeException("Error reading secure config", ex);
            }
        } else if (this.isIntegrationtest(env)) {
            try {
                T config = clazz.getDeclaredConstructor().newInstance();
                PropertiesConfigUtil.loadFromEnv(config, env, "application.secure-config.", keys);
                return config;
            } catch (Exception ex) {
                throw new RuntimeException("Unable to create new config object", ex);
            }
        } else {
            int count = 0;
            while (Files.notExists(Paths.get(this.ephemeralKeyFile)) || Files.notExists(Paths.get(this.ephemeralConfigFile))) {
                count++;
                if (count >= this.ephemeralTimeout) {
                    throw new RuntimeException("Key and/or Secrets files does not exist");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Thread interrupted while waiting for secrets.");
                }
            }
            return EncryptedConfigUtil.read(this.ephemeralKeyFile, this.ephemeralConfigFile, clazz);
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
