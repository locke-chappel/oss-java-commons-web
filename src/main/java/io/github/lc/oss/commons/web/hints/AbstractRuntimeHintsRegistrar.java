package io.github.lc.oss.commons.web.hints;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

public abstract class AbstractRuntimeHintsRegistrar implements RuntimeHintsRegistrar {
    private static final Set<TypeReference> NO_REFLECTIONS = Collections.unmodifiableSet(new HashSet<>(0));
    private static final Set<TypeReference> DEFAULT_REFLECTIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList( //
            TypeReference.of(io.github.lc.oss.commons.util.PathNormalizer.class), //
            TypeReference.of(io.github.lc.oss.commons.web.advice.AbstractControllerAdvice.class), //
            TypeReference.of(io.github.lc.oss.commons.web.advice.AbstractSystemIdentityAdvice.class), //
            TypeReference.of(io.github.lc.oss.commons.web.advice.CommonAdvice.class), //
            TypeReference.of(io.github.lc.oss.commons.web.advice.ETagAdvice.class), //
            TypeReference.of(io.github.lc.oss.commons.web.annotations.SystemIdentity.class), //
            TypeReference.of(io.github.lc.oss.commons.web.annotations.HttpCachable.class), //
            TypeReference.of(org.springframework.stereotype.Controller.class), //
            TypeReference.of(org.springframework.web.bind.annotation.RestController.class), //
            TypeReference.of(org.springframework.web.bind.annotation.RequestMapping.class), //
            TypeReference.of(org.springframework.web.bind.annotation.GetMapping.class), //
            TypeReference.of(org.springframework.web.bind.annotation.PostMapping.class), //
            TypeReference.of(org.springframework.web.bind.annotation.PatchMapping.class), //
            TypeReference.of(org.springframework.web.bind.annotation.PutMapping.class), //
            TypeReference.of(org.springframework.web.bind.annotation.DeleteMapping.class) //
    )));

    private static final Set<List<TypeReference>> NO_PROXIES = Collections.unmodifiableSet(new HashSet<>(0));

    private static final Set<String> NO_RESOURCE_BUNDLES = Collections.unmodifiableSet(new HashSet<>(0));
    private static final Set<String> DEFAULT_RESOURCE_BUNDLES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList( //
            "org.aspectj.weaver.weaver-messages", //
            "com.google.javascript.jscomp.parsing.ParserConfig")));

    protected Set<TypeReference> getDefaultReflectionClasses() {
        return AbstractRuntimeHintsRegistrar.DEFAULT_REFLECTIONS;
    }

    protected Set<TypeReference> getReflectionClasses() {
        return AbstractRuntimeHintsRegistrar.NO_REFLECTIONS;
    }

    protected Set<List<TypeReference>> getProxyClasses() {
        return AbstractRuntimeHintsRegistrar.NO_PROXIES;
    }

    protected Set<String> getDefaultResourceBundles() {
        return AbstractRuntimeHintsRegistrar.DEFAULT_RESOURCE_BUNDLES;
    }

    protected Set<String> getResourceBundles() {
        return AbstractRuntimeHintsRegistrar.NO_RESOURCE_BUNDLES;
    }

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (TypeReference ref : this.getDefaultReflectionClasses()) {
            hints.reflection().registerType(ref, MemberCategory.values());
        }

        for (TypeReference ref : this.getReflectionClasses()) {
            hints.reflection().registerType(ref, MemberCategory.values());
        }

        for (List<TypeReference> proxy : this.getProxyClasses()) {
            hints.proxies().registerJdkProxy(proxy.toArray(new TypeReference[proxy.size()]));
        }

        for (String bundle : this.getDefaultResourceBundles()) {
            hints.resources().registerResourceBundle(bundle);
        }

        for (String bundle : this.getResourceBundles()) {
            hints.resources().registerResourceBundle(bundle);
        }
    }
}
