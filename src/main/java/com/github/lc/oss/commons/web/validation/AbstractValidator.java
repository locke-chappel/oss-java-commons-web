package com.github.lc.oss.commons.web.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.lc.oss.commons.serialization.Message;

public abstract class AbstractValidator<Type> implements Validator<Type> {

    protected boolean matches(Pattern pattern, String value) {
        return this.matches(pattern, value, true);
    }

    protected boolean matches(Pattern pattern, String value, boolean required) {
        if (value == null) {
            return !required;
        }

        return pattern.matcher(value).find();
    }

    protected <T> boolean missingValue(T instance) {
        if (instance == null) {
            return true;
        }

        if (instance instanceof String && ((String) instance).trim().equals("")) {
            return true;
        }

        if (instance instanceof Collection && ((Collection<?>) instance).isEmpty()) {
            return true;
        }

        return false;
    }

    protected <T> void merge(Set<Message> messages, Validator<T> validator, T value) {
        messages.addAll(validator.validate(value));
    }

    protected Set<Message> valid() {
        return new HashSet<>();
    }
}
