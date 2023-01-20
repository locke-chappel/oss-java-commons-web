package com.github.lc.oss.commons.web.hints;

import java.util.Set;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public abstract class AbstractRuntimeHintsRegistrar implements RuntimeHintsRegistrar {
    protected abstract Set<Class<?>> getClasses();

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (MemberCategory mc : MemberCategory.values()) {
            for (Class<?> clazz : this.getClasses()) {
                hints.reflection().registerType(clazz, mc);
            }
        }
    }
}
