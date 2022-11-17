package com.github.lc.oss.commons.web.advice;

import org.aspectj.lang.annotation.Pointcut;

public abstract class AbstractControllerAdvice {
    @Pointcut("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController)")
    public void inAnyController() {
    }

    @Pointcut("execution(public org.springframework.http.ResponseEntity * (..))")
    public void returnsResponseEntity() {
    }

    @Pointcut("execution(public org.springframework.web.servlet.ModelAndView * (..))")
    public void returnsModelAndView() {
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) ||" + //
            "@annotation(org.springframework.web.bind.annotation.GetMapping) ||" + //
            "@annotation(org.springframework.web.bind.annotation.PostMapping) ||" + //
            "@annotation(org.springframework.web.bind.annotation.PatchMapping) ||" + //
            "@annotation(org.springframework.web.bind.annotation.PutMapping) ||" + //
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void withRequestMapping() {
    }
}
