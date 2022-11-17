package com.github.lc.oss.commons.web.tokens;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.lc.oss.commons.encryption.Ciphers;
import com.github.lc.oss.commons.hashing.Hashes;

public class EncryptedStatelessCsrfTokenManager extends StatelessCsrfTokenManager {
    private char[] cipherKey = Hashes.SHA2_512.hash(UUID.randomUUID().toString()).toCharArray();

    /**
     * Default is a randomized cipher key per startup, this does not support
     * clustered solutions. Override if this is the needed behavior.
     */
    protected char[] getCipherKey() {
        return this.cipherKey;
    }

    @Override
    protected Token fromJson(String cipheredToken) {
        if (cipheredToken == null || cipheredToken.trim().equals("")) {
            return null;
        }

        try {
            return StatelessCsrfTokenManager.JSON_READER.readValue(Ciphers.AES128.decryptString(cipheredToken, this.getCipherKey()));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Error reading Token JSON.", ex);
        }
    }

    @Override
    protected String toJson(Token token) {
        if (token == null) {
            return null;
        }

        try {
            return Ciphers.AES128.encrypt(StatelessCsrfTokenManager.JSON_WRITER.writeValueAsString(token), this.getCipherKey());
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Error writing Token JSON.", ex);
        }
    }
}
