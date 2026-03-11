package io.github.lc.oss.commons.web.util;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.encryption.config.ConfigKey;
import io.github.lc.oss.commons.encryption.config.EncryptedConfig;
import io.github.lc.oss.commons.encryption.config.EncryptedConfigUtil;

public class ConfigLoader {
    public static <T extends EncryptedConfig, K extends ConfigKey> T loadFromFile(String keyFile, String configFile,
            long timeout, Class<T> clazz) {
        int count = 0;
        while (Files.notExists(Paths.get(keyFile)) || Files.notExists(Paths.get(configFile))) {
            count++;
            if (count >= timeout) {
                throw new RuntimeException(
                        "Error loading encrypted config from file: Key and/or Secrets files does not exist");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(
                        "Error loading encrypted config from file: Thread interrupted while waiting for secrets.");
            }
        }
        return EncryptedConfigUtil.read(keyFile, configFile, clazz);
    }

    public static <T extends EncryptedConfig, K extends ConfigKey> T loadFromProperties(Environment env, K[] keys,
            Class<T> clazz) {
        try {
            T config = clazz.getDeclaredConstructor().newInstance();
            PropertiesConfigUtil.loadFromEnv(config, env, "application.secure-config.", keys);
            return config;
        } catch (Exception ex) {
            throw new RuntimeException("Error loading encrypted config from properties files", ex);
        }
    }

    public static <T extends EncryptedConfig, K extends ConfigKey> T loadJsonFromEnv(Environment env, String envVar,
            Class<T> clazz) {
        try {
            String json = env.getProperty("CONFIG", "");
            if (json.charAt(0) != '{') {
                json = Encodings.Base64.decodeString(json);
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing encrypted config JSON from env variable", ex);
        }
    }

    private ConfigLoader() {
    }
}
