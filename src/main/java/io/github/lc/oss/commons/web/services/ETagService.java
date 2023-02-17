package io.github.lc.oss.commons.web.services;

public interface ETagService {
    String getETag(String id);

    void evictETag(String id);

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void clearCache();
}
