package com.github.lc.oss.commons.web.tokens;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CsrfToken implements Token {
    private final String id;
    private final long expires;

    public CsrfToken(int expires) {
        this.id = UUID.randomUUID().toString();
        this.expires = System.currentTimeMillis() + expires;
    }

    public CsrfToken(@JsonProperty("id") String id, @JsonProperty("expires") long expires) {
        this.id = id;
        this.expires = expires;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public long getExpires() {
        return this.expires;
    }
}
