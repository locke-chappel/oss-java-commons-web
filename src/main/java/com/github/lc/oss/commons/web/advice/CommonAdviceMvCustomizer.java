package com.github.lc.oss.commons.web.advice;

import org.springframework.web.servlet.ModelAndView;

public interface CommonAdviceMvCustomizer {
    ModelAndView customize(ModelAndView mv);
}
