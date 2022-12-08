package com.github.lc.oss.commons.web.filters;

import java.io.IOException;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;

import com.github.lc.oss.commons.l10n.L10N;
import com.github.lc.oss.commons.l10n.UserLocale;
import com.github.lc.oss.commons.testing.AbstractMockTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UserLocaleFilterTest extends AbstractMockTest {
    @Mock
    private L10N l10n;

    @InjectMocks
    private UserLocaleFilter filter;

    @BeforeEach
    public void init() {
        UserLocale userLocale = new UserLocale();
        this.setField("userLocale", userLocale, this.filter);
    }

    @Test
    public void test_doFilter_noHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(this.l10n.getDefaultLocale()).thenReturn(Locale.GERMAN);
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).thenReturn(null);

        /* UserLocale defaults to English in the constructor */
        Assertions.assertEquals(Locale.ENGLISH, this.filter.getUserLocale().getLocale());

        try {
            this.filter.doFilter(request, response, chain);
        } catch (IOException | ServletException e) {
            Assertions.fail("Unexpected exception");
        }

        /*
         * The default locale from L10N should have been set on the UserLocale instance
         */
        Assertions.assertEquals(Locale.GERMAN, this.filter.getUserLocale().getLocale());
    }

    @Test
    public void test_doFilter_emptyHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(this.l10n.getDefaultLocale()).thenReturn(Locale.GERMAN);
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).thenReturn("");

        /* UserLocale defaults to English in the constructor */
        Assertions.assertEquals(Locale.ENGLISH, this.filter.getUserLocale().getLocale());

        try {
            this.filter.doFilter(request, response, chain);
        } catch (IOException | ServletException e) {
            Assertions.fail("Unexpected exception");
        }

        /*
         * The default locale from L10N should have been set on the UserLocale instance
         */
        Assertions.assertEquals(Locale.GERMAN, this.filter.getUserLocale().getLocale());
    }

    @Test
    public void test_doFilter_blankHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(this.l10n.getDefaultLocale()).thenReturn(Locale.GERMAN);
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).thenReturn(" \t \r \n ");

        /* UserLocale defaults to English in the constructor */
        Assertions.assertEquals(Locale.ENGLISH, this.filter.getUserLocale().getLocale());

        try {
            this.filter.doFilter(request, response, chain);
        } catch (IOException | ServletException e) {
            Assertions.fail("Unexpected exception");
        }

        /*
         * The default locale from L10N should have been set on the UserLocale instance
         */
        Assertions.assertEquals(Locale.GERMAN, this.filter.getUserLocale().getLocale());
    }

    @Test
    public void test_doFilter_badHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(this.l10n.getDefaultLocale()).thenReturn(Locale.GERMAN);
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).thenReturn("fake_language");

        /* UserLocale defaults to English in the constructor */
        Assertions.assertEquals(Locale.ENGLISH, this.filter.getUserLocale().getLocale());

        try {
            this.filter.doFilter(request, response, chain);
        } catch (IOException | ServletException e) {
            Assertions.fail("Unexpected exception");
        }

        /*
         * The default locale from L10N should have been set on the UserLocale instance
         */
        Assertions.assertEquals(Locale.GERMAN, this.filter.getUserLocale().getLocale());
    }

    @Test
    public void test_doFilter_unsupportedLanguage() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(this.l10n.getDefaultLocale()).thenReturn(Locale.GERMAN);
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).thenReturn(Locale.FRENCH.toLanguageTag());
        Mockito.when(this.l10n.hasLocale(Locale.FRENCH)).thenReturn(false);

        /* UserLocale defaults to English in the constructor */
        Assertions.assertEquals(Locale.ENGLISH, this.filter.getUserLocale().getLocale());

        try {
            this.filter.doFilter(request, response, chain);
        } catch (IOException | ServletException e) {
            Assertions.fail("Unexpected exception");
        }

        /*
         * The default locale from L10N should have been set on the UserLocale instance
         */
        Assertions.assertEquals(Locale.GERMAN, this.filter.getUserLocale().getLocale());
    }

    @Test
    public void test_doFilter_supportedLanguage() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(this.l10n.getDefaultLocale()).thenReturn(Locale.GERMAN);
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).thenReturn(Locale.FRENCH.toLanguageTag());
        Mockito.when(this.l10n.hasLocale(Locale.FRENCH)).thenReturn(true);

        /* UserLocale defaults to English in the constructor */
        Assertions.assertEquals(Locale.ENGLISH, this.filter.getUserLocale().getLocale());

        try {
            this.filter.doFilter(request, response, chain);
        } catch (IOException | ServletException e) {
            Assertions.fail("Unexpected exception");
        }

        /*
         * The user requested locale should be set on the UserLocale instance
         */
        Assertions.assertEquals(Locale.FRENCH, this.filter.getUserLocale().getLocale());
    }
}
