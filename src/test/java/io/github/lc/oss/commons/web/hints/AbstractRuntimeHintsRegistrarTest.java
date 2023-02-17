package io.github.lc.oss.commons.web.hints;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;

import io.github.lc.oss.commons.testing.AbstractMockTest;

public class AbstractRuntimeHintsRegistrarTest extends AbstractMockTest {
    private static class TestClass extends AbstractRuntimeHintsRegistrar {
        @Override
        protected Set<Class<?>> getClasses() {
            return new HashSet<>(Arrays.asList(TestClass.class));
        }
    }

    @Test
    public void test_registerHints() {
        AbstractRuntimeHintsRegistrar registrar = new TestClass();

        RuntimeHints hints = Mockito.mock(RuntimeHints.class);
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        ReflectionHints reflection = Mockito.mock(ReflectionHints.class);

        Mockito.when(hints.reflection()).thenReturn(reflection);

        registrar.registerHints(hints, classLoader);
    }
}
