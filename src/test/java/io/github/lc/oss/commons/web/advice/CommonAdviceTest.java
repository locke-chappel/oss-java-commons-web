package io.github.lc.oss.commons.web.advice;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.servlet.ModelAndView;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.l10n.UserLocale;
import io.github.lc.oss.commons.testing.AbstractMockTest;

public class CommonAdviceTest extends AbstractMockTest {
    @Mock
    private L10N l10n;
    @Mock
    private UserLocale userLocale;

    private CommonAdvice advice;

    @BeforeEach
    public void init() {
        this.advice = new CommonAdvice();
        this.setField("l10n", this.l10n, this.advice);
        this.setField("userLocale", this.userLocale, this.advice);
    }

    @Test
    public void test_getVars_exception() {
        try {
            this.advice.getVars(Locale.ENGLISH);
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals("Illegal state - expected cache to be built before calling this method", ex.getMessage());
        }
    }

    @Test
    public void test_getText_empty_cached() {
        this.advice.setCaching(true);

        Map<String, String> all = this.advice.getText(Locale.ENGLISH);
        Assertions.assertNotNull(all);
        Assertions.assertTrue(all.isEmpty());
        Set<String> vars = this.advice.getVars(Locale.ENGLISH);
        Assertions.assertNotNull(vars);
        Assertions.assertTrue(vars.isEmpty());
        Map<String, String> allCached = this.advice.getText(Locale.ENGLISH);
        Assertions.assertNotNull(allCached);
        Assertions.assertTrue(allCached.isEmpty());
        Assertions.assertSame(all, allCached);
        Set<String> varsCached = this.advice.getVars(Locale.ENGLISH);
        Assertions.assertNotNull(varsCached);
        Assertions.assertTrue(varsCached.isEmpty());
        Assertions.assertSame(vars, varsCached);
    }

    @Test
    public void test_getText_empty_notCached() {
        this.advice.setCaching(false);
        this.setField("l10n", this.l10n, this.advice);
        this.setField("userLocale", this.userLocale, this.advice);

        Map<String, String> all = this.advice.getText(Locale.ENGLISH);
        Assertions.assertNotNull(all);
        Assertions.assertTrue(all.isEmpty());
        Set<String> vars = this.advice.getVars(Locale.ENGLISH);
        Assertions.assertNotNull(vars);
        Assertions.assertTrue(vars.isEmpty());
        Map<String, String> allCached = this.advice.getText(Locale.ENGLISH);
        Assertions.assertNotNull(allCached);
        Assertions.assertTrue(allCached.isEmpty());
        Assertions.assertNotSame(all, allCached);
        Set<String> varsCached = this.advice.getVars(Locale.ENGLISH);
        Assertions.assertNotNull(varsCached);
        Assertions.assertTrue(varsCached.isEmpty());
        Assertions.assertNotSame(vars, varsCached);
    }

    @Test
    public void test_getText() {
        Map<String, String> text = new HashMap<>();
        text.put("a", "A");
        text.put("a.b", "A %Var%");
        Mockito.when(this.l10n.getAll(Locale.ENGLISH)).thenReturn(text);

        Map<String, String> all = this.advice.getText(Locale.ENGLISH);
        Assertions.assertNotNull(all);
        Assertions.assertEquals("A", all.get("a"));
        Assertions.assertEquals("A %Var%", all.get("a_b"));
        Assertions.assertNull(all.get("a.b"));

        Set<String> vars = this.advice.getVars(Locale.ENGLISH);
        Assertions.assertNotNull(vars);
        Assertions.assertEquals(1, vars.size());
        Assertions.assertTrue(vars.contains("a_b"));
    }

    @Test
    public void test_modelAndView_exception() {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);

        try {
            Mockito.when(pjp.proceed()).thenThrow(new Exception("boom!"));
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }

        try {
            this.advice.modelAndView(pjp);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("java.lang.Exception: boom!", ex.getMessage());
        }
    }

    @Test
    public void test_modelAndView() {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        ModelAndView mv = Mockito.mock(ModelAndView.class);

        try {
            Mockito.when(pjp.proceed()).thenReturn(mv);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(this.userLocale.getLocale()).thenReturn(Locale.ENGLISH);
        Map<String, String> text = new HashMap<>();
        text.put("a", "A");
        text.put("a.b", "A %Var%");
        Mockito.when(this.l10n.getAll(Locale.ENGLISH)).thenReturn(text);

        ModelAndView result = (ModelAndView) this.advice.modelAndView(pjp);
        Assertions.assertSame(mv, result);
    }

    @Test
    public void test_modelAndView_null() {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);

        try {
            Mockito.when(pjp.proceed()).thenReturn(null);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }

        ModelAndView result = (ModelAndView) this.advice.modelAndView(pjp);
        Assertions.assertNull(result);
    }

    @Test
    public void test_modelAndView_withCustomization() {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        ModelAndView mv = Mockito.mock(ModelAndView.class);

        this.setField("customizer", new CommonAdviceMvCustomizer() {
            @Override
            public ModelAndView customize(ModelAndView mv) {
                mv.addObject("custom", "value");
                return mv;
            }
        }, this.advice);

        try {
            Mockito.when(pjp.proceed()).thenReturn(mv);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.when(this.userLocale.getLocale()).thenReturn(Locale.ENGLISH);
        Map<String, String> text = new HashMap<>();
        text.put("a", "A");
        text.put("a.b", "A %Var%");
        Mockito.when(this.l10n.getAll(Locale.ENGLISH)).thenReturn(text);

        ModelAndView result = (ModelAndView) this.advice.modelAndView(pjp);
        Assertions.assertSame(mv, result);
    }
}
