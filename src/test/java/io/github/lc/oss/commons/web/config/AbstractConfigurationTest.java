package io.github.lc.oss.commons.web.config;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.encryption.config.ConfigKey;
import io.github.lc.oss.commons.encryption.config.EncryptedConfig;
import io.github.lc.oss.commons.testing.AbstractMockTest;
import io.github.lc.oss.commons.util.IoTools;

public class AbstractConfigurationTest extends AbstractMockTest {
    private static class TestConfig extends AbstractConfiguration {

    }

    /*
     * This inner class must be public so that the ConfigLoader can create an
     * instance otherwise the EncryptedConfig tests will fail
     */
    public static class TestSecure extends EncryptedConfig {
        private enum TestKeys implements ConfigKey {
            Key1, Key2;

            @Override
            public Class<?> type() {
                return String.class;
            }
        }

        public TestSecure() {
            super(TestKeys.class);
        }
    }

    @Test
    public void test_beans() {
        AbstractConfiguration config = new TestConfig();

        Assertions.assertNotNull(config.clock());
        Assertions.assertNotNull(config.exceptionController());
        Assertions.assertNotNull(config.pathNormalizer());
        Assertions.assertNotNull(config.trimmingModule());
        Assertions.assertNotNull(config.securityHeadersFilter());
        Assertions.assertNotNull(config.sessionRegistry());
    }

    @Test
    public void test_configureDefaultPublicAccessUrls() {
        AbstractConfiguration config = new TestConfig();

        @SuppressWarnings("unchecked")
        HttpSecurity http = new HttpSecurity(Mockito.mock(ObjectPostProcessor.class),
                Mockito.mock(AuthenticationManagerBuilder.class), new HashMap<>());

        try {
            config.configureDefaultPublicAccessUrls(http);
        } catch (Exception ex) {
            Assertions.fail("Unexpected exception", ex);
        }
    }

    @Test
    public void test_configurePathMatch() {
        PathMatchConfigurer configurer = Mockito.mock(PathMatchConfigurer.class);

        AbstractConfiguration config = new TestConfig();

        config.configurePathMatch(configurer);
    }

    @Test
    public void test_loadEncryptedConfig_it() {
        AbstractConfiguration config = new TestConfig();

        Environment env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty("knative", Boolean.class, Boolean.FALSE)).thenReturn(false);
        Mockito.when(env.getProperty("integrationtest", Boolean.class, Boolean.FALSE)).thenReturn(true);
        Mockito.when(env.getProperty("application.secure-config.Key1")).thenReturn("integration");
        Mockito.when(env.getProperty("application.secure-config.Key2")).thenReturn("9001");

        TestSecure result = config.loadEncryptedConfig(env, TestSecure.TestKeys.values(), TestSecure.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("integration", result.get(TestSecure.TestKeys.Key1));
        Assertions.assertEquals("9001", result.get(TestSecure.TestKeys.Key2));
    }

    @Test
    public void test_loadEncryptedConfig_knative_bae64() {
        AbstractConfiguration config = new TestConfig();

        Environment env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty("knative", Boolean.class, Boolean.FALSE)).thenReturn(true);
        Mockito.when(env.getProperty("CONFIG", ""))
                .thenReturn(Encodings.Base64.encode("{\"Key1\" : \"knative\", \"Key2\" : 100 }"));

        TestSecure result = config.loadEncryptedConfig(env, TestSecure.TestKeys.values(), TestSecure.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("knative", result.get(TestSecure.TestKeys.Key1));
        Assertions.assertEquals(100, result.get(TestSecure.TestKeys.Key2));
    }

    @Test
    public void test_loadEncryptedConfig_default() {
        AbstractConfiguration config = new TestConfig();
        this.setField("ephemeralKeyFile", IoTools.getAbsoluteFilePath("test.key"), config);
        this.setField("ephemeralConfigFile", IoTools.getAbsoluteFilePath("test.config"), config);
        this.setField("ephemeralTimeout", 5, config);

        Environment env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty("knative", Boolean.class, Boolean.FALSE)).thenReturn(false);
        Mockito.when(env.getProperty("integrationtest", Boolean.class, Boolean.FALSE)).thenReturn(false);

        TestSecure result = config.loadEncryptedConfig(env, TestSecure.TestKeys.values(), TestSecure.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Encrypted", result.get(TestSecure.TestKeys.Key1));
        Assertions.assertEquals(1, result.get(TestSecure.TestKeys.Key2));
    }
}
