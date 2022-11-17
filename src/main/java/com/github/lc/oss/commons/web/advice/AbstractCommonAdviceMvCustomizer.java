package com.github.lc.oss.commons.web.advice;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.lc.oss.commons.l10n.L10N;
import com.github.lc.oss.commons.l10n.UserLocale;
import com.github.lc.oss.commons.l10n.Variable;

public abstract class AbstractCommonAdviceMvCustomizer implements CommonAdviceMvCustomizer {
    @Autowired(required = false)
    private UserLocale userLocale;
    @Autowired(required = false)
    private L10N l10n;

    protected UserLocale getUserLocale() {
        return this.userLocale;
    }

    protected L10N getL10n() {
        return this.l10n;
    }

    protected Locale getCurrentLocale() {
        if (this.getL10n() == null) {
            return java.util.Locale.ENGLISH;
        }

        Locale locale = this.getL10n().getDefaultLocale();
        if (this.getUserLocale() != null) {
            locale = this.getUserLocale().getLocale();
        }
        return locale;
    }

    protected String getText(String id, Variable... vars) {
        if (this.getL10n() == null) {
            return id;
        }
        return this.getL10n().getText(this.getCurrentLocale(), id, vars);
    }
}
