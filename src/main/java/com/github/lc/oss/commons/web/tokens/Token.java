package com.github.lc.oss.commons.web.tokens;

import com.github.lc.oss.commons.serialization.Jsonable;

public interface Token extends Jsonable {
    String getId();

    long getExpires();
}
