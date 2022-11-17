package com.github.lc.oss.commons.web.validation;

import java.util.Set;

import com.github.lc.oss.commons.serialization.Message;

public interface Validator<Type> {
    Set<Message> validate(Type instance);
}
