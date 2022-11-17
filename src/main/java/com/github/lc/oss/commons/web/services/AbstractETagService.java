package com.github.lc.oss.commons.web.services;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.github.lc.oss.commons.encoding.Encodings;
import com.github.lc.oss.commons.hashing.Hashes;

public abstract class AbstractETagService implements ETagService {
    private static class ETag {
        static final String FORMAT = "W/\"%s\"";

        private final String id;
        private final long expiration;
        private final String value;

        public ETag(Clock clock, String id, String version) {
            if (clock == null) {
                throw new RuntimeException("Clock cannot be null");
            }

            if (version == null || version.trim().equals("")) {
                throw new RuntimeException("Version cannot be blank");
            }

            this.id = id;
            Instant hour = clock.instant().truncatedTo(ChronoUnit.HOURS);
            this.expiration = hour.plus(1, ChronoUnit.HOURS).toEpochMilli();
            String value = id + version + hour.toString();
            this.value = String.format(ETag.FORMAT, Hashes.MD5.hash(value, Encodings.Base64));
        }

        public String getId() {
            return this.id;
        }

        public String getValue() {
            return this.value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > this.expiration;
        }
    }

    private final Map<String, ETag> cache = new HashMap<>();
    @Value("${application.services.etag.enabled:true}")
    private boolean enabled;

    @Autowired(required = false)
    private Clock clock;

    protected Clock getClock() {
        return this.clock;
    }

    protected abstract String getAppVersion();

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void clearCache() {
        this.cache.clear();
    }

    @Override
    public String getETag(String id) {
        String key = id == null ? null : id.trim();
        if (key == null) {
            throw new IllegalArgumentException("id cannot be null/blank");
        }

        if (this.isEnabled()) {
            ETag etag = this.cache.get(key);
            if (etag == null || etag.isExpired()) {
                etag = new ETag(this.getClock(), key, this.getAppVersion());
                this.cache.put(etag.getId(), etag);
            }
            return etag.getValue();
        } else {
            return String.format(ETag.FORMAT, Long.toString(System.currentTimeMillis()) + "\"");
        }
    }

    @Override
    public void evictETag(String id) {
        this.cache.remove(id);
    }
}
