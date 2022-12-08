package com.github.lc.oss.commons.web.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.lc.oss.commons.testing.AbstractMockTest;

import jakarta.servlet.ServletContext;

public class ContextUtilTest extends AbstractMockTest {
    @Test
    public void test_getAbsoluteUrl() {
        ServletContext context = Mockito.mock(ServletContext.class);

        Mockito.when(context.getContextPath()).thenReturn("");

        String result = ContextUtil.getAbsoluteUrl(null, context);
        Assertions.assertEquals("/", result);

        result = ContextUtil.getAbsoluteUrl("", context);
        Assertions.assertEquals("/", result);

        result = ContextUtil.getAbsoluteUrl(" ", context);
        Assertions.assertEquals("/", result);

        result = ContextUtil.getAbsoluteUrl(" \t \r \n \t ", context);
        Assertions.assertEquals("/", result);

        result = ContextUtil.getAbsoluteUrl("/", context);
        Assertions.assertEquals("/", result);
    }

    @Test
    public void test_getAbsoluteUrl_nullContextPath() {
        ServletContext context = Mockito.mock(ServletContext.class);

        Mockito.when(context.getContextPath()).thenReturn(null);

        String result = ContextUtil.getAbsoluteUrl("page", context);
        Assertions.assertEquals("/page", result);
    }

    @Test
    public void test_getAbsoluteUrl_nullContext() {
        try {
            ContextUtil.getAbsoluteUrl(null, null);
            Assertions.fail("Expected Exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("context is required", ex.getMessage());
        }
    }

    @Test
    public void test_getAbsoluteUrl_noSlashes() {
        ServletContext context = Mockito.mock(ServletContext.class);

        Mockito.when(context.getContextPath()).thenReturn("appName");

        String result = ContextUtil.getAbsoluteUrl("index", context);
        Assertions.assertEquals("/appName/index", result);
    }
}
