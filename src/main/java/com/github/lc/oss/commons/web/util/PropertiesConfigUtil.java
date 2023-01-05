package com.github.lc.oss.commons.web.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.env.Environment;

import com.github.lc.oss.commons.encryption.config.ConfigKey;
import com.github.lc.oss.commons.encryption.config.EncryptedConfig;
import com.github.lc.oss.commons.encryption.config.EncryptedConfig.User;
import com.github.lc.oss.commons.encryption.config.EncryptedConfigUtil;

/**
 * WARNING: This tool is meant for use in integration and development type
 * scenarios where the "secure" config is being parsed from the loaded java
 * properties configuration. While this class is safe to use in production code
 * (the code itself isn't the issue), it would imply that you may be storing
 * sensitive data in clear text which is not good.<br />
 * <br />
 * Only use this class to load non-sensitive data. Production use cases likely
 * should use process outlined by the {@linkplain EncryptedConfigUtil} instead.
 */
public class PropertiesConfigUtil {
    private PropertiesConfigUtil() {
    }

    public static void loadFromEnv(EncryptedConfig config, Environment env, String keyPrefix, ConfigKey[] keys) {
        for (ConfigKey key : keys) {
            Object value = PropertiesConfigUtil.getValueFromEnv(env, keyPrefix, key);
            if (value != null) {
                config.set(key, value);
            }
        }
    }

    public static Object getValueFromEnv(Environment env, String keyPrefix, ConfigKey key) {
        if (String.class.equals(key.type())) {
            return env.getProperty(keyPrefix + key.name());
        } else if (Integer.class.equals(key.type())) {
            return env.getProperty(keyPrefix + key.name(), Integer.class);
        } else if (List.class.equals(key.type())) {
            String s;
            int i = 0;
            List<String> dataList = new ArrayList<>();
            while ((s = env.getProperty(keyPrefix + key.name() + "." + Integer.toString(i))) != null) {
                dataList.add(s);
                i++;
            }
            if (dataList.isEmpty()) {
                return null;
            }
            return dataList;
        } else if (User.class.equals(key.type())) {
            String username = env.getProperty(keyPrefix + key.name() + ".username");
            String password = env.getProperty(keyPrefix + key.name() + ".password");
            if (username != null || password != null) {
                return new User(username, password);
            }
            return null;
        }

        return null;
    }
}
