package io.github.lc.oss.commons.web.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.encryption.config.ConfigKey;
import io.github.lc.oss.commons.encryption.config.EncryptedConfig;
import io.github.lc.oss.commons.testing.AbstractMockTest;
import io.github.lc.oss.commons.util.IoTools;

public class ConfigLoaderTest extends AbstractMockTest {
    private static class TestSecure extends EncryptedConfig {
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

    private static class Explosive extends EncryptedConfig {
        public Explosive() {
            super(null);
        }
    }

    private static class ThreadHelper implements Runnable {
        private Thread parent;

        public ThreadHelper(Thread parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Test was interrupted unexpectedly", ex);
            }
            this.parent.interrupt();
        }
    }

    @Test
    public void test_loadEncryptedConfig_it() {
        Environment env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty("application.secure-config.Key1")).thenReturn("integration");
        Mockito.when(env.getProperty("application.secure-config.Key2")).thenReturn("9001");

        TestSecure result = ConfigLoader.loadFromProperties(env, TestSecure.TestKeys.values(), TestSecure.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("integration", result.get(TestSecure.TestKeys.Key1));
        Assertions.assertEquals("9001", result.get(TestSecure.TestKeys.Key2));
    }

    @Test
    public void test_loadEncryptedConfig_it_error() {
        Environment env = Mockito.mock(Environment.class);

        try {
            ConfigLoader.loadFromProperties(env, TestSecure.TestKeys.values(), Explosive.class);
            Assertions.fail("Expected Exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error loading encrypted config from properties files", ex.getMessage());
        }
    }

    @Test
    public void test_loadEncryptedConfig_knative_json() {
        Environment env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty("CONFIG", "")).thenReturn("{\"Key1\" : \"knative\", \"Key2\" : 100 }");

        TestSecure result = ConfigLoader.loadJsonFromEnv(env, "CONFIG", TestSecure.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("knative", result.get(TestSecure.TestKeys.Key1));
        Assertions.assertEquals(100, result.get(TestSecure.TestKeys.Key2));
    }

    @Test
    public void test_loadEncryptedConfig_knative_bae64() {
        Environment env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty("CONFIG", ""))
                .thenReturn(Encodings.Base64.encode("{\"Key1\" : \"knative\", \"Key2\" : 100 }"));

        TestSecure result = ConfigLoader.loadJsonFromEnv(env, "CONFIG", TestSecure.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("knative", result.get(TestSecure.TestKeys.Key1));
        Assertions.assertEquals(100, result.get(TestSecure.TestKeys.Key2));
    }

    @Test
    public void test_loadEncryptedConfig_knative_blankJson() {
        Environment env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty("CONFIG", "")).thenReturn("");

        try {
            ConfigLoader.loadJsonFromEnv(env, "CONFIG", TestSecure.class);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error parsing encrypted config JSON from env variable", ex.getMessage());
        }
    }

    @Test
    public void test_loadEncryptedConfig_knative_error() {
        Environment env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty("CONFIG", "")).thenReturn("not JSON");

        try {
            ConfigLoader.loadJsonFromEnv(env, "CONFIG", TestSecure.class);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error parsing encrypted config JSON from env variable", ex.getMessage());
        }
    }

    @Test
    public void test_loadEncryptedConfig_default() {
        TestSecure result = ConfigLoader.loadFromFile(IoTools.getAbsoluteFilePath("test.key"),
                IoTools.getAbsoluteFilePath("test.config"), 5, TestSecure.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Encrypted", result.get(TestSecure.TestKeys.Key1));
        Assertions.assertEquals(1, result.get(TestSecure.TestKeys.Key2));
    }

    @Test
    public void test_loadEncryptedConfig_default_badConfig() {
        try {
            ConfigLoader.loadFromFile(IoTools.getAbsoluteFilePath("test.key"),
                    IoTools.getAbsoluteFilePath("test-bad.config"), 5, TestSecure.class);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error reading secure config", ex.getMessage());
        }
    }

    @Test
    public void test_loadEncryptedConfig_default_error() {
        try {
            ConfigLoader.loadFromFile("junkPath", IoTools.getAbsoluteFilePath("test.config"), 1,
                    TestSecure.class);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error loading encrypted config from file: Key and/or Secrets files does not exist",
                    ex.getMessage());
        }
    }

    @Test
    public void test_loadEncryptedConfig_default_error_v2() {
        try {
            ConfigLoader.loadFromFile(IoTools.getAbsoluteFilePath("test.key"), "junkPath", 2, TestSecure.class);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error loading encrypted config from file: Key and/or Secrets files does not exist",
                    ex.getMessage());
        }
    }

    @Test
    public void test_loadEncryptedConfig_default_interrupted() {
        Thread t = new Thread(new ThreadHelper(Thread.currentThread()));

        t.start();
        try {
            ConfigLoader.loadFromFile("/junk/path", "/junk/path", 120, TestSecure.class);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals(
                    "Error loading encrypted config from file: Thread interrupted while waiting for secrets.",
                    ex.getMessage());
        }
    }
}
