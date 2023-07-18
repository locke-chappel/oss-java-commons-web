package io.github.lc.oss.commons.web.hints;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import io.github.lc.oss.commons.testing.AbstractMockTest;

public class AbstractRuntimeHintsRegistrarTest extends AbstractMockTest {

    @Test
    public void test_registerHints_nothing() {
        AbstractRuntimeHintsRegistrar registrar = new AbstractRuntimeHintsRegistrar() {
        };

        RuntimeHints runtimeHints = Mockito.mock(RuntimeHints.class);
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        ReflectionHints reflectionHints = Mockito.mock(ReflectionHints.class);
        ResourceHints resourceHints = Mockito.mock(ResourceHints.class);

        Mockito.when(runtimeHints.reflection()).thenReturn(reflectionHints);
        Mockito.when(runtimeHints.resources()).thenReturn(resourceHints);

        registrar.registerHints(runtimeHints, classLoader);
    }

    @Test
    public void test_registerHints() {
        AbstractRuntimeHintsRegistrar registrar = new AbstractRuntimeHintsRegistrar() {
            @Override
            protected Set<TypeReference> getReflectionClasses() {
                return new HashSet<>(Arrays.asList(TypeReference.of(AbstractRuntimeHintsRegistrar.class)));
            }

            @Override
            protected Set<List<TypeReference>> getProxyClasses() {
                return new HashSet<>(Arrays.asList(Arrays.asList(TypeReference.of(AbstractRuntimeHintsRegistrar.class))));
            }

            @Override
            protected Set<String> getResourceBundles() {
                return new HashSet<>(Arrays.asList("res-bundle"));
            }
        };

        RuntimeHints runtimeHints = Mockito.mock(RuntimeHints.class);
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        ReflectionHints reflectionHints = Mockito.mock(ReflectionHints.class);
        ProxyHints proxyHints = Mockito.mock(ProxyHints.class);
        ResourceHints resourceHints = Mockito.mock(ResourceHints.class);

        Mockito.when(runtimeHints.reflection()).thenReturn(reflectionHints);
        Mockito.when(runtimeHints.proxies()).thenReturn(proxyHints);
        Mockito.when(runtimeHints.resources()).thenReturn(resourceHints);

        registrar.registerHints(runtimeHints, classLoader);
    }
}
