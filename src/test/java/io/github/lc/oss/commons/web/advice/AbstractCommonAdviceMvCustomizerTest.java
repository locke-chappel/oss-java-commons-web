package io.github.lc.oss.commons.web.advice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.servlet.ModelAndView;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.l10n.UserLocale;
import io.github.lc.oss.commons.testing.AbstractMockTest;

public class AbstractCommonAdviceMvCustomizerTest extends AbstractMockTest {
    private static class TestClass extends AbstractCommonAdviceMvCustomizer {
        @Override
        public ModelAndView customize(ModelAndView mv) {
            return mv;
        }
    }

    @Mock
    private L10N l10n;
    @Mock
    private UserLocale userLocale;

    @Test
    public void test_getText_nullL10n() {
        AbstractCommonAdviceMvCustomizer customizer = new TestClass();
        this.setField("l10n", null, customizer);

        String result = customizer.getText("random");
        Assertions.assertEquals("random", result);
    }

    @Test
    public void test_getCurrentLocale_nullL10n() {
        AbstractCommonAdviceMvCustomizer customizer = new TestClass();
        this.setField("l10n", null, customizer);

        java.util.Locale result = customizer.getCurrentLocale();
        Assertions.assertEquals(java.util.Locale.ENGLISH, result);
    }

    @Test
    public void test_getText_nullUserLocale() {
        AbstractCommonAdviceMvCustomizer customizer = new TestClass();
        this.setField("l10n", this.l10n, customizer);
        this.setField("userLocale", null, customizer);

        Mockito.when(this.l10n.getDefaultLocale()).thenReturn(java.util.Locale.CANADA);
        Mockito.when(this.l10n.getText(java.util.Locale.CANADA, "random")).thenReturn("Text");

        String result = customizer.getText("random");
        Assertions.assertEquals("Text", result);
    }

    @Test
    public void test_getText_withUserLocale() {
        AbstractCommonAdviceMvCustomizer customizer = new TestClass();
        this.setField("l10n", this.l10n, customizer);
        this.setField("userLocale", this.userLocale, customizer);

        Mockito.when(this.l10n.getDefaultLocale()).thenReturn(java.util.Locale.CANADA);
        Mockito.when(this.userLocale.getLocale()).thenReturn(java.util.Locale.GERMAN);
        Mockito.when(this.l10n.getText(java.util.Locale.GERMAN, "random")).thenReturn("Text");

        String result = customizer.getText("random");
        Assertions.assertEquals("Text", result);
    }
}
