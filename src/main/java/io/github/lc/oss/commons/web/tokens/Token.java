package io.github.lc.oss.commons.web.tokens;

import io.github.lc.oss.commons.serialization.Jsonable;

public interface Token extends Jsonable {
    String getId();

    long getExpires();
}
