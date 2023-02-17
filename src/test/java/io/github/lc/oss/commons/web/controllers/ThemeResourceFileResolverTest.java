package io.github.lc.oss.commons.web.controllers;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.github.lc.oss.commons.testing.AbstractMockTest;
import io.github.lc.oss.commons.util.PathNormalizer;
import io.github.lc.oss.commons.web.resources.AbstractResourceResolver.Types;

public class ThemeResourceFileResolverTest extends AbstractMockTest {
    @Mock
    private UserTheme userTheme;

    private PathNormalizer pathNormalizer = new PathNormalizer();

    private ThemeResourceFileResolver resolver;

    @BeforeEach
    public void init() {
        this.resolver = new ThemeResourceFileResolver("root", 2);
        this.setField("pathNormalizer", this.pathNormalizer, this.resolver);
        this.setField("userTheme", this.userTheme, this.resolver);
    }

    @Test
    public void test_getTheme_noUserTheme() {
        this.setField("userTheme", null, this.resolver);
        List<String> result = this.resolver.findFiles(Types.css);
        Assertions.assertNull(result);
    }

    @Test
    public void test_getTheme_userThemeNotSet_null() {
        this.setField("userTheme", this.userTheme, this.resolver);

        Mockito.when(this.userTheme.getName()).thenReturn(null);

        List<String> result = this.resolver.findFiles(Types.css);
        Assertions.assertNull(result);
    }

    @Test
    public void test_getRoot() {
        Mockito.when(this.userTheme.getName()).thenReturn("theme");

        String result = this.resolver.getRoot();
        Assertions.assertEquals("root/theme/", result);
    }

    @Test
    public void test_findFiles_noTheme() {
        Mockito.when(this.userTheme.getName()).thenReturn(null);

        List<String> result = this.resolver.findFiles(Types.css, null);
        Assertions.assertNull(result);
    }

    @Test
    public void test_findFiles() {
        Mockito.when(this.userTheme.getName()).thenReturn("theme");

        List<String> result = this.resolver.findFiles(Types.css, null);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }
}
