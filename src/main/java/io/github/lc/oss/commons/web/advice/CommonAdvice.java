package io.github.lc.oss.commons.web.advice;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.l10n.UserLocale;
import io.github.lc.oss.commons.l10n.Variable;

@Aspect
public class CommonAdvice extends AbstractControllerAdvice {
    private Map<Locale, Map<String, String>> textCache = new HashMap<>();
    private Map<Locale, Set<String>> varCache = new HashMap<>();

    @Autowired
    private L10N l10n;
    @Autowired
    private UserLocale userLocale;
    @Autowired(required = false)
    private CommonAdviceMvCustomizer customizer;

    @Value("${application.ui.caching:true}")
    private boolean caching;

    public void setCaching(boolean enabled) {
        this.caching = enabled;
    }

    public boolean enableCaching() {
        return this.caching;
    }

    @Around("inAnyController() && returnsModelAndView() && withRequestMapping()")
    public Object modelAndView(final ProceedingJoinPoint method) {
        ModelAndView mv = null;
        try {
            mv = (ModelAndView) method.proceed();
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }

        if (mv != null) {
            Locale locale = this.getUserLocale().getLocale();
            Map<String, String> text = new HashMap<>(this.getText(locale));
            Set<String> vars = this.getVars(locale);
            for (String id : vars) {
                text.put(id, this.resolve(text.get(id)));
            }
            mv.addAllObjects(text);
        }

        if (this.customizer != null) {
            mv = this.customizer.customize(mv);
        }
        return mv;
    }

    private String resolve(String text) {
        String value = text;
        Matcher m = Variable.HTML_ID.matcher(text);
        while (m.find()) {
            value = this.replace(value, m.group(1), m.group(2));
        }

        return value;
    }

    private String replace(String text, String key, String id) {
        return text;
    }

    Map<String, String> getText(Locale locale) {
        Map<String, String> cache = this.textCache.get(locale);
        if (cache == null || !this.enableCaching()) {
            this.updateCache(locale);
            cache = this.textCache.get(locale);
        }
        return cache;
    }

    Set<String> getVars(Locale locale) {
        Set<String> cache = this.varCache.get(locale);
        Assert.notNull(cache, "Illegal state - expected cache to be built before calling this method");
        return cache;
    }

    private void updateCache(Locale locale) {
        Map<String, String> text = new HashMap<>();
        Set<String> vars = new HashSet<>();
        Matcher m;
        String id;
        for (Entry<String, String> e : this.getL10N().getAll(locale).entrySet()) {
            id = e.getKey().replace(".", "_");
            text.put(id, e.getValue());
            m = Variable.HTML_ID.matcher(e.getValue());
            if (m.find()) {
                vars.add(id);
            }
        }
        this.textCache.put(locale, Collections.unmodifiableMap(text));
        this.varCache.put(locale, Collections.unmodifiableSet(vars));
    }

    private L10N getL10N() {
        return this.l10n;
    }

    private UserLocale getUserLocale() {
        return this.userLocale;
    }
}
