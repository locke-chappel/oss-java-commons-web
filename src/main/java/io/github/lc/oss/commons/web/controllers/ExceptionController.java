package io.github.lc.oss.commons.web.controllers;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;

import io.github.lc.oss.commons.serialization.JsonMessage;
import io.github.lc.oss.commons.serialization.JsonableHashSet;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.serialization.Response;
import io.github.lc.oss.commons.web.config.Authorities;
import io.github.lc.oss.commons.web.util.ContextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@PreAuthorize(Authorities.PUBLIC)
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ExceptionController extends AbstractController {
    @Autowired(required = false)
    private AccessDeniedHandler accessDeniedHandler;

    protected JsonMessage getErrorMessage() {
        return this.toMessage(Message.Categories.Application, Message.Severities.Error, 1);
    }

    @ExceptionHandler(value = { Exception.class, RuntimeException.class })
    @ResponseBody
    public ResponseEntity<Response<?>> catchException(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

        Throwable ex = this.getCause(exception);
        if (this.insufficientPermissions(ex)) {
            try {
                if (this.accessDeniedHandler != null) {
                    this.accessDeniedHandler.handle(request, response, (AccessDeniedException) ex);
                } else {
                    response.sendRedirect(ContextUtil.getAbsoluteUrl("/", request.getServletContext()));
                }
            } catch (IOException | ServletException e) {
                this.getLogger().error("Error sending redirect", e);
            }
            return null;
        } else if (this.isBadRequest(ex)) {
            this.getLogger().error("Bad request", ex);
        } else if (this.isMethodNotSupported(ex)) {
            status = HttpStatus.METHOD_NOT_ALLOWED;
            this.getLogger().error("Method not allowed", ex);
        } else if (this.isMediaNotSupported(ex)) {
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            this.getLogger().error("Unsupported media type", ex);
        } else if (this.isMediaNotAcceptable(ex)) {
            status = HttpStatus.NOT_ACCEPTABLE;
            this.getLogger().error("Not Acceptable", ex);
        } else if (this.is404(ex)) {
            status = HttpStatus.NOT_FOUND;
            this.getLogger().error("Not Found", ex);
        } else {
            this.getLogger().error("Unhandled exception", ex);
        }
        return new ResponseEntity<>(new Response<>(new JsonableHashSet<>(Arrays.asList(this.getErrorMessage()))), status);
    }

    private Throwable getCause(Throwable ex) {
        if (ex == null) {
            return ex;
        }

        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private boolean insufficientPermissions(Throwable throwable) {
        return this.is(throwable, AccessDeniedException.class);
    }

    private boolean isBadRequest(Throwable throwable) {
        return this.is(throwable, HttpMessageNotReadableException.class);
    }

    private boolean isMethodNotSupported(Throwable throwable) {
        return this.is(throwable, HttpRequestMethodNotSupportedException.class);
    }

    private boolean isMediaNotAcceptable(Throwable throwable) {
        return this.is(throwable, HttpMediaTypeNotAcceptableException.class);
    }

    private boolean isMediaNotSupported(Throwable throwable) {
        return this.is(throwable, HttpMediaTypeNotSupportedException.class);
    }

    private boolean is404(Throwable throwable) {
        return this.is(throwable, NoHandlerFoundException.class);
    }

    private boolean is(Throwable throwable, Class<?> a) {
        if (throwable == null) {
            return false;
        }
        return a.isAssignableFrom(throwable.getClass());
    }
}
