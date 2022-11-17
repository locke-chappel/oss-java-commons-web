package com.github.lc.oss.commons.web.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpCachable {
    /**
     * Custom cache key value. Default is blank (dynamically generated).
     */
    String value() default "";
}
