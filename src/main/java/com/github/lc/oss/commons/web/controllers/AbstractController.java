package com.github.lc.oss.commons.web.controllers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.github.lc.oss.commons.l10n.L10N;
import com.github.lc.oss.commons.l10n.UserLocale;
import com.github.lc.oss.commons.l10n.Variable;
import com.github.lc.oss.commons.serialization.JsonMessage;
import com.github.lc.oss.commons.serialization.Jsonable;
import com.github.lc.oss.commons.serialization.JsonableCollection;
import com.github.lc.oss.commons.serialization.JsonableMap;
import com.github.lc.oss.commons.serialization.Message;
import com.github.lc.oss.commons.serialization.Message.Severities;
import com.github.lc.oss.commons.serialization.Response;
import com.github.lc.oss.commons.web.services.ETagService;

public abstract class AbstractController {
    private static final Logger logger = LoggerFactory.getLogger(AbstractController.class);

    @Autowired(required = false)
    private ETagService etagService;
    @Autowired(required = false)
    private L10N l10n;
    @Autowired(required = false)
    private UserLocale userLocale;

    protected ETagService getETagService() {
        return this.etagService;
    }

    protected L10N getL10n() {
        return this.l10n;
    }

    protected Logger getLogger() {
        return AbstractController.logger;
    }

    protected UserLocale getUserLocale() {
        return this.userLocale;
    }

    protected <T> ResponseEntity<T> noContent() {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    protected <T> ResponseEntity<T> notFound() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    protected <T extends Jsonable> ResponseEntity<Response<T>> respond(T body) {
        if (body == null || //
                body instanceof JsonableCollection<?> && ((JsonableCollection<?>) body).isEmpty() || //
                body instanceof JsonableMap<?> && ((JsonableMap<?>) body).isEmpty()) //
        {
            return this.noContent();
        }
        return this.respond(new Response<>(body));
    }

    protected <T extends Response<? extends Jsonable>> ResponseEntity<T> respond(Message... messages) {
        return this.respond(Arrays.asList(messages));
    }

    @SuppressWarnings("unchecked")
    protected <T extends Response<? extends Jsonable>> ResponseEntity<T> respond(Collection<Message> messages) {
        Set<Message> errors = messages.stream(). //
                filter(m -> m != null). //
                filter(m -> m.getSeverity() != null). //
                filter(m -> m.getSeverity().name().equals(Severities.Error.name())). //
                collect(Collectors.toSet());
        if (!errors.isEmpty()) {
            return new ResponseEntity<>((T) new Response<>(errors), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return this.respond((T) new Response<>(messages));
    }

    protected <T extends Response<? extends Jsonable>> ResponseEntity<T> respond(T response) {
        return this.respond(response, (HttpHeaders) null);
    }

    protected <T extends Response<? extends Jsonable>> ResponseEntity<T> respond(T response, HttpHeaders headers) {
        HttpStatus status = HttpStatus.OK;
        boolean hasBody = response.getBody() != null;
        boolean hasMessages = response.hasMessages();
        if (response.hasSeverity(Message.Severities.Error)) {
            return this.respond(response.getMessages());
        } else if (!hasBody && !hasMessages) {
            status = HttpStatus.NO_CONTENT;
        } else if (response.hasSeverity(Message.Severities.Warning)) {
            status = HttpStatus.ACCEPTED;
        }

        if (headers != null) {
            return new ResponseEntity<>(response, headers, status);
        }
        return new ResponseEntity<>(response, status);
    }

    protected <T extends Response<? extends Jsonable>> ResponseEntity<T> respond(T response, String etagId) {
        HttpHeaders headers = null;
        ETagService eTagService = this.getETagService();
        if (eTagService != null) {
            headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noCache().cachePrivate());
            headers.setETag(eTagService.getETag(etagId));
        }
        return this.respond(response, headers);
    }

    protected <T extends Response<? extends Jsonable>> ResponseEntity<T> respond(Message.Category category, Message.Severity severity, int number,
            Variable... vars) {
        return this.respond(this.toMessage(category, severity, number, vars));
    }

    protected ResponseEntity<InputStreamResource> respond(InputStreamResource content, Map<String, String> headers) {
        HttpHeaders responseHeaders = new HttpHeaders();
        for (Entry<String, String> entry : headers.entrySet()) {
            responseHeaders.set(entry.getKey(), entry.getValue());
        }

        return new ResponseEntity<>(content, responseHeaders, HttpStatus.OK);
    }

    protected Message toMessage(Message message, Variable... vars) {
        return this.toMessage(message.getCategory(), message.getSeverity(), message.getNumber(), vars);
    }

    protected Message toMessage(Message.Category category, Message.Severity severity, int number, Variable... vars) {
        String text = null;
        if (this.getL10n() != null) {
            text = this.getText(String.format("messages.%s.%s.%d", category.name(), severity.name(), number), vars);
        }
        return new JsonMessage(category, severity, number, text);
    }

    protected Locale getCurrentLocale() {
        Locale locale = this.getL10n().getDefaultLocale();
        if (this.getUserLocale() != null) {
            locale = this.getUserLocale().getLocale();
        }
        return locale;
    }

    protected String getText(String id, Variable... vars) {
        return this.getL10n().getText(this.getCurrentLocale(), id, vars);
    }
}
