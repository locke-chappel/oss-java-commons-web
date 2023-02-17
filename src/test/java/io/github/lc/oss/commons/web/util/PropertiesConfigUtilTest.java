package io.github.lc.oss.commons.web.util;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import io.github.lc.oss.commons.encryption.config.ConfigKey;
import io.github.lc.oss.commons.encryption.config.EncryptedConfig;
import io.github.lc.oss.commons.encryption.config.EncryptedConfig.User;
import io.github.lc.oss.commons.testing.AbstractMockTest;

public class PropertiesConfigUtilTest extends AbstractMockTest {
    private static class TestConfig extends EncryptedConfig {
        public enum Keys implements ConfigKey {
            String(String.class),
            Integer(Integer.class),
            User(User.class),
            UserNameOnly(User.class),
            UserPassOnly(User.class),
            UserNulls(User.class),
            List(List.class),
            EmptyList(List.class),
            Missing(String.class),
            UnsupportedType(TestConfig.class);

            private Class<?> type;

            private Keys(Class<?> type) {
                this.type = type;
            }

            @Override
            public Class<?> type() {
                return this.type;
            }
        }

        public TestConfig() {
            super(Keys.class);
        }

    }

    @Test
    public void test_loadFromEnv() {
        TestConfig config = new TestConfig();

        for (TestConfig.Keys key : TestConfig.Keys.values()) {
            Assertions.assertNull(config.get(key));
        }

        final String prefix = "config.";

        Environment env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty(prefix + "String")).thenReturn("String-Value");
        Mockito.when(env.getProperty(prefix + "Integer", Integer.class)).thenReturn(100);
        Mockito.when(env.getProperty(prefix + "List.0")).thenReturn("List-Value-0");
        Mockito.when(env.getProperty(prefix + "List.1")).thenReturn("List-Value-1");
        Mockito.when(env.getProperty(prefix + "List.2")).thenReturn(null);
        Mockito.when(env.getProperty(prefix + "EmptyList.0")).thenReturn(null);
        Mockito.when(env.getProperty(prefix + "User.username")).thenReturn("User-username-value");
        Mockito.when(env.getProperty(prefix + "User.password")).thenReturn("User-password-value");
        Mockito.when(env.getProperty(prefix + "UserNameOnly.username")).thenReturn("UserNameOnly-username-value");
        Mockito.when(env.getProperty(prefix + "UserNameOnly.password")).thenReturn(null);
        Mockito.when(env.getProperty(prefix + "UserPassOnly.username")).thenReturn(null);
        Mockito.when(env.getProperty(prefix + "UserPassOnly.password")).thenReturn("UserPassOnly-password-value");
        Mockito.when(env.getProperty(prefix + "UserNulls.username")).thenReturn(null);
        Mockito.when(env.getProperty(prefix + "UserNulls.password")).thenReturn(null);
        Mockito.when(env.getProperty(prefix + "Missing")).thenReturn(null);

        PropertiesConfigUtil.loadFromEnv(config, env, prefix, TestConfig.Keys.values());

        Assertions.assertEquals("String-Value", config.get(TestConfig.Keys.String));
        Assertions.assertEquals(Integer.valueOf(100), config.get(TestConfig.Keys.Integer));
        Object value = config.get(TestConfig.Keys.List);
        Assertions.assertTrue(value instanceof List);
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) value;
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("List-Value-0", list.get(0));
        Assertions.assertEquals("List-Value-1", list.get(1));
        value = config.get(TestConfig.Keys.User);
        Assertions.assertTrue(value instanceof User);
        User user = (User) value;
        Assertions.assertEquals("User-username-value", user.getUsername());
        Assertions.assertEquals("User-password-value", user.getPassword());
        value = config.get(TestConfig.Keys.UserNameOnly);
        Assertions.assertTrue(value instanceof User);
        user = (User) value;
        Assertions.assertEquals("UserNameOnly-username-value", user.getUsername());
        Assertions.assertNull(user.getPassword());
        value = config.get(TestConfig.Keys.UserPassOnly);
        Assertions.assertTrue(value instanceof User);
        user = (User) value;
        Assertions.assertNull(user.getUsername());
        Assertions.assertEquals("UserPassOnly-password-value", user.getPassword());
        value = config.get(TestConfig.Keys.UserNulls);
        Assertions.assertNull(value);
        Assertions.assertNull(config.get(TestConfig.Keys.EmptyList));
        Assertions.assertNull(config.get(TestConfig.Keys.Missing));
    }
}
