package com.github.lc.oss.commons.web.controllers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerMapping;

import com.github.lc.oss.commons.l10n.L10N;
import com.github.lc.oss.commons.serialization.PrimitiveMap;
import com.github.lc.oss.commons.testing.AbstractMockTest;
import com.github.lc.oss.commons.util.IoTools;
import com.github.lc.oss.commons.web.resources.AbstractResourceResolver;
import com.github.lc.oss.commons.web.resources.AbstractResourceResolver.Types;
import com.github.lc.oss.commons.web.resources.Minifier;
import com.github.lc.oss.commons.web.resources.StaticResourceFileResolver;
import com.github.lc.oss.commons.web.services.ETagService;
import com.github.lc.oss.commons.web.tokens.CsrfTokenManager;

import jakarta.servlet.ServletContext;

public class ResourceControllerTest extends AbstractMockTest {
    private static class TestController extends ResourceController {

    }

    @Test
    public void test_getConsoleLogPrefix_null() {
        ResourceController controller = new TestController();
        Assertions.assertNull(controller.getConsoleLogPrefix());
    }

    @Test
    public void test_getContextPath_null() {
        ResourceController controller = new TestController();
        Assertions.assertNull(controller.getContextPath());
    }

    @Test
    public void test_respond_noEtagService() {
        ResourceController controller = new TestController();
        final String content = "/* content */";

        ResponseEntity<String> result = controller.respond(content, "etag-id");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertSame(content, result.getBody());
        Assertions.assertNull(result.getHeaders().getCacheControl());
        Assertions.assertNull(result.getHeaders().getETag());
    }

    @Test
    public void test_respond() {
        final ETagService etagService = Mockito.mock(ETagService.class);
        ResourceController controller = new ResourceController() {
            @Override
            protected ETagService getETagService() {
                return etagService;
            }
        };
        Mockito.when(etagService.getETag("etag-id")).thenReturn("W/\"etag\"");
        final String content = "/* content */";

        ResponseEntity<String> result = controller.respond(content, "etag-id");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertSame(content, result.getBody());
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
    }

    @Test
    public void test_minify() {
        final Minifier minifier = Mockito.mock(Minifier.class);
        ResourceController controllerWithMinifier = new ResourceController() {
            @Override
            protected Minifier getMinifier() {
                return minifier;
            }
        };
        ResourceController controllerNoMinifier = new TestController();
        Assertions.assertSame(minifier, controllerWithMinifier.getMinifier());
        Assertions.assertNull(controllerNoMinifier.getMinifier());

        final String css = "body {\\n\\tcolor: #FFFFFF;\\n}\\ndiv {\\n\\t/* emtpy */\\n}";
        final String expectedCss = "body{color: #fff}";
        final String js = "var $$ = {\n\t Function : function() {\n\tvar aLongVariableName = \"test\";\n\tdocument.write(aLongVariableName);\n}};\n\n$$.Function();\n";
        final String expectedJs = "'use strict';var $$={Function:function(){document.write(\"test\")}};$$.Function();";

        Mockito.when(minifier.minifyCssIfEnabled(css)).thenReturn(expectedCss);
        Mockito.when(minifier.minifyJsIfEnabled(js)).thenReturn(expectedJs);

        Assertions.assertEquals(expectedCss, controllerWithMinifier.minify(Types.css, css));
        Assertions.assertEquals(expectedJs, controllerWithMinifier.minify(Types.js, js));
        Assertions.assertSame(js, controllerWithMinifier.minify(Types.img, js));

        Assertions.assertSame(css, controllerNoMinifier.minify(Types.css, css));
        Assertions.assertSame(js, controllerNoMinifier.minify(Types.js, js));
        Assertions.assertSame(js, controllerNoMinifier.minify(Types.img, js));
    }

    @Test
    public void test_get_nullType() {
        ResourceController controller = new TestController();

        try {
            controller.get(null);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Type cannot be null", ex.getMessage());
        }
    }

    @Test
    public void test_get_notCssOrJs() {
        ResourceController controller = new TestController();

        try {
            controller.get(Types.img);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("This version of get() only supports CSS and JavaScript.", ex.getMessage());
        }
    }

    @Test
    public void test_get_defaults() {
        ResourceController controller = new ResourceController() {
            @Override
            protected List<StaticResourceFileResolver> getFileResolvers() {
                return Arrays.asList( //
                        AbstractResourceResolver.LIBRARY_RESOLVER, //
                        new StaticResourceFileResolver("static-app-variable", 2));
            }

            @Override
            protected String getConsoleLogPrefix() {
                return "";
            }

            @Override
            protected String getContextPath() {
                return "";
            }
        };

        String testJs = new String(IoTools.readFile("static-app-variable/js/vars.js"), StandardCharsets.UTF_8);
        Assertions.assertNotNull(testJs);
        testJs = testJs.replaceAll("#context.cookies.prefix#", "");
        testJs = testJs.replaceAll("#context.logging.prefix#", "");
        testJs = testJs.replaceAll("#context.path.resource#", "");
        testJs = testJs.replaceAll("#context.path.url#", "");
        testJs = testJs.replaceAll("#context.timeout#", "-1");
        testJs = testJs.replaceAll("#context.timeout.enabled#", "false");
        testJs = testJs.replaceAll("#id#", "#id#");
        testJs = testJs.replaceAll("#junk#", "#junk#");

        ResponseEntity<String> result = controller.get(Types.js);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("$$.Init"));
        Assertions.assertFalse(result.getBody().contains("$$$"));
        Assertions.assertNull(result.getHeaders().getCacheControl());
        Assertions.assertNull(result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testJs));
    }

    @Test
    public void test_get() {
        final ETagService etagService = Mockito.mock(ETagService.class);
        final WebRequest request = Mockito.mock(WebRequest.class);
        final ServletContext context = Mockito.mock(ServletContext.class);
        final L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected List<StaticResourceFileResolver> getFileResolvers() {
                return Arrays.asList(//
                        AbstractResourceResolver.LIBRARY_RESOLVER, //
                        new StaticResourceFileResolver("static-app-variable", 2));
            }

            @Override
            protected ETagService getETagService() {
                return etagService;
            }

            @Override
            protected String getConsoleLogPrefix() {
                return "App ";
            }

            @Override
            protected String getCookiePrefix() {
                return "__Secure-";
            }

            @Override
            protected L10N getL10N() {
                return l10n;
            }

            @Override
            protected boolean isCaching() {
                return false;
            }
        };
        this.setField("context", context, controller);

        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(l10n.getText(Locale.ENGLISH, "id")).thenReturn("text");
        Mockito.when(l10n.getText(Locale.ENGLISH, "junk")).thenReturn(null);

        Mockito.when(etagService.getETag("-" + Types.js.name())).thenReturn("W/\"etag\"");
        Mockito.when(etagService.getETag("-" + Types.css.name())).thenReturn("W/\"etag\"");
        Mockito.when(request.checkNotModified("W/\"etag\"")).thenReturn(false);

        String testJs = new String(IoTools.readFile("static-app-variable/js/vars.js"), StandardCharsets.UTF_8);
        Assertions.assertNotNull(testJs);
        testJs = testJs.replaceAll("#context.cookies.prefix#", "__Secure-");
        testJs = testJs.replaceAll("#context.path.resource#", "/");
        testJs = testJs.replaceAll("#context.path.url#", "/");
        testJs = testJs.replaceAll("#context.logging.prefix#", "App");
        testJs = testJs.replaceAll("#context.timeout#", "-1");
        testJs = testJs.replaceAll("#context.timeout.enabled#", "false");
        testJs = testJs.replaceAll("#id#", "text");
        testJs = testJs.replaceAll("#junk#", "#junk#");

        String testCss = new String(IoTools.readFile("static-app-variable/css/vars.css"), StandardCharsets.UTF_8);
        Assertions.assertNotNull(testCss);
        testCss = testCss.replaceAll("#context.path.resource#", "/");
        testCss = testCss.replaceAll("#context.path.url#", "/");

        // -- first request, uncached
        ResponseEntity<String> result = controller.js(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("$$.Init"));
        Assertions.assertFalse(result.getBody().contains("$$$"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testJs));
        final String initialJs = result.getBody();

        result = controller.css(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("background-image"));
        Assertions.assertFalse(result.getBody().contains("#context.path#"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testCss));
        final String initialCss = result.getBody();

        // -- second request, uncached
        result = controller.js(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("$$.Init"));
        Assertions.assertFalse(result.getBody().contains("$$$"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testJs));
        Assertions.assertNotSame(initialJs, result.getBody());
        Assertions.assertEquals(initialJs, result.getBody());

        result = controller.css(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("background-image"));
        Assertions.assertFalse(result.getBody().contains("#context.path#"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testCss));
        Assertions.assertNotSame(initialCss, result.getBody());
        Assertions.assertEquals(initialCss, result.getBody());
    }

    @Test
    public void test_get_withTheme() {
        final ETagService etagService = Mockito.mock(ETagService.class);
        final WebRequest request = Mockito.mock(WebRequest.class);
        final ServletContext context = Mockito.mock(ServletContext.class);
        final L10N l10n = Mockito.mock(L10N.class);
        final UserTheme userTheme = Mockito.mock(UserTheme.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected List<StaticResourceFileResolver> getFileResolvers() {
                return Arrays.asList(//
                        AbstractResourceResolver.LIBRARY_RESOLVER, //
                        new StaticResourceFileResolver("static-app-variable", 2));
            }

            @Override
            protected ETagService getETagService() {
                return etagService;
            }

            @Override
            protected String getConsoleLogPrefix() {
                return "App ";
            }

            @Override
            protected String getCookiePrefix() {
                return "__Host-";
            }

            @Override
            protected L10N getL10N() {
                return l10n;
            }

            @Override
            protected boolean isCaching() {
                return false;
            }
        };
        this.setField("context", context, controller);
        this.setField("userTheme", userTheme, controller);

        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(l10n.getText(Locale.ENGLISH, "id")).thenReturn("text");
        Mockito.when(l10n.getText(Locale.ENGLISH, "junk")).thenReturn(null);

        Mockito.when(userTheme.getName()).thenReturn("theme-name");

        Mockito.when(etagService.getETag("theme-name-" + Types.js.name())).thenReturn("W/\"etag\"");
        Mockito.when(etagService.getETag("theme-name-" + Types.css.name())).thenReturn("W/\"etag\"");
        Mockito.when(request.checkNotModified("W/\"etag\"")).thenReturn(false);

        String testJs = new String(IoTools.readFile("static-app-variable/js/vars.js"), StandardCharsets.UTF_8);
        Assertions.assertNotNull(testJs);
        testJs = testJs.replaceAll("#context.cookies.prefix#", "__Host-");
        testJs = testJs.replaceAll("#context.path.resource#", "/");
        testJs = testJs.replaceAll("#context.path.url#", "/");
        testJs = testJs.replaceAll("#context.logging.prefix#", "App");
        testJs = testJs.replaceAll("#context.timeout#", "-1");
        testJs = testJs.replaceAll("#context.timeout.enabled#", "false");
        testJs = testJs.replaceAll("#id#", "text");
        testJs = testJs.replaceAll("#junk#", "#junk#");

        String testCss = new String(IoTools.readFile("static-app-variable/css/vars.css"), StandardCharsets.UTF_8);
        Assertions.assertNotNull(testCss);
        testCss = testCss.replaceAll("#context.path.resource#", "/");
        testCss = testCss.replaceAll("#context.path.url#", "/");

        // -- first request, uncached
        ResponseEntity<String> result = controller.js(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("$$.Init"));
        Assertions.assertFalse(result.getBody().contains("$$$"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testJs));
        final String initialJs = result.getBody();

        result = controller.css(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("background-image"));
        Assertions.assertFalse(result.getBody().contains("#context.path.url#"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testCss));
        final String initialCss = result.getBody();

        // -- second request, uncached
        result = controller.js(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("$$.Init"));
        Assertions.assertFalse(result.getBody().contains("$$$"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testJs));
        Assertions.assertNotSame(initialJs, result.getBody());
        Assertions.assertEquals(initialJs, result.getBody());

        result = controller.css(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("background-image"));
        Assertions.assertFalse(result.getBody().contains("#context.path.resources#"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testCss));
        Assertions.assertNotSame(initialCss, result.getBody());
        Assertions.assertEquals(initialCss, result.getBody());
    }

    @Test
    public void test_get_caching() {
        final ETagService etagService = Mockito.mock(ETagService.class);
        final WebRequest request = Mockito.mock(WebRequest.class);
        final ServletContext context = Mockito.mock(ServletContext.class);
        final L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected List<StaticResourceFileResolver> getFileResolvers() {
                return Arrays.asList(//
                        AbstractResourceResolver.LIBRARY_RESOLVER, //
                        new StaticResourceFileResolver("static-app-variable", 2));
            }

            @Override
            protected String getCacheKeyPrefix() {
                return "test-user-role";
            }

            @Override
            protected ETagService getETagService() {
                return etagService;
            }

            @Override
            protected String getConsoleLogPrefix() {
                return "App ";
            }

            @Override
            protected String getCookiePrefix() {
                return "__Secure-";
            }

            @Override
            protected L10N getL10N() {
                return l10n;
            }
        };
        this.setField("enableCaching", true, controller);
        this.setField("context", context, controller);

        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(l10n.getText(Locale.ENGLISH, "id")).thenReturn("text");
        Mockito.when(l10n.getText(Locale.ENGLISH, "junk")).thenReturn(null);

        Mockito.when(etagService.getETag(controller.getCacheKeyPrefix() + "-" + Types.js.name())).thenReturn("W/\"etag\"");
        Mockito.when(etagService.getETag(controller.getCacheKeyPrefix() + "-" + Types.css.name())).thenReturn("W/\"etag\"");
        Mockito.when(request.checkNotModified("W/\"etag\"")).thenReturn(false);

        String testJs = new String(IoTools.readFile("static-app-variable/js/vars.js"), StandardCharsets.UTF_8);
        Assertions.assertNotNull(testJs);
        testJs = testJs.replaceAll("#context.cookies.prefix#", "__Secure-");
        testJs = testJs.replaceAll("#context.path.resource#", "/");
        testJs = testJs.replaceAll("#context.path.url#", "/");
        testJs = testJs.replaceAll("#context.logging.prefix#", "App");
        testJs = testJs.replaceAll("#context.timeout#", "-1");
        testJs = testJs.replaceAll("#context.timeout.enabled#", "false");
        testJs = testJs.replaceAll("#id#", "text");
        testJs = testJs.replaceAll("#junk#", "#junk#");

        String testCss = new String(IoTools.readFile("static-app-variable/css/vars.css"), StandardCharsets.UTF_8);
        Assertions.assertNotNull(testCss);
        testCss = testCss.replaceAll("#context.path.resource#", "/");
        testCss = testCss.replaceAll("#context.path.url#", "/");

        // -- first request, uncached
        ResponseEntity<String> result = controller.get(Types.js, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("$$.Init"));
        Assertions.assertFalse(result.getBody().contains("$$$"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testJs));
        final String initialJs = result.getBody();

        result = controller.get(Types.css, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("background-image"));
        Assertions.assertFalse(result.getBody().contains("#context.path.url#"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testCss));
        final String initialCss = result.getBody();

        // -- second request, cached
        result = controller.get(Types.js, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("$$.Init"));
        Assertions.assertFalse(result.getBody().contains("$$$"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testJs));
        Assertions.assertSame(initialJs, result.getBody());

        result = controller.get(Types.css, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("background-image"));
        Assertions.assertFalse(result.getBody().contains("#context.path.resources#"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testCss));
        Assertions.assertSame(initialCss, result.getBody());

        // -- after clearing uncached again
        controller.clearCache();

        result = controller.get(Types.js, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("$$.Init"));
        Assertions.assertFalse(result.getBody().contains("$$$"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testJs));
        Assertions.assertNotSame(initialJs, result.getBody());
        Assertions.assertEquals(initialJs, result.getBody());

        result = controller.get(Types.css, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertTrue(result.getBody().contains("background-image"));
        Assertions.assertFalse(result.getBody().contains("#context.path#"));
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertTrue(result.getBody().contains(testCss));
        Assertions.assertNotSame(initialCss, result.getBody());
        Assertions.assertEquals(initialCss, result.getBody());
    }

    @Test
    public void test_get_notModified() {
        ETagService etagService = Mockito.mock(ETagService.class);
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected ETagService getETagService() {
                return etagService;
            }
        };

        Mockito.when(etagService.getETag("-" + Types.js.name())).thenReturn("W/\"etag\"");
        Mockito.when(request.checkNotModified("W/\"etag\"")).thenReturn(true);

        ResponseEntity<String> result = controller.get(Types.js, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_MODIFIED, result.getStatusCode());
        Assertions.assertNull(result.getHeaders().getCacheControl());
        Assertions.assertNull(result.getHeaders().getETag());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_checkEtag_noService() {
        ResourceController controller = new TestController();

        Assertions.assertFalse(controller.checkEtag(null, "id"));

        WebRequest request = Mockito.mock(WebRequest.class);
        Assertions.assertFalse(controller.checkEtag(request, "id"));
    }

    @Test
    public void test_checkEtag() {
        final ETagService etagService = Mockito.mock(ETagService.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected ETagService getETagService() {
                return etagService;
            }
        };

        Mockito.when(etagService.getETag("match")).thenReturn("W/\"tag\"");
        Mockito.when(etagService.getETag("noMatch")).thenReturn("W/\"tag-2\"");

        Assertions.assertFalse(controller.checkEtag(null, "match"));

        WebRequest request = Mockito.mock(WebRequest.class);
        Mockito.when(request.checkNotModified("W/\"tag-2\"")).thenReturn(false);
        Assertions.assertFalse(controller.checkEtag(request, "noMatch"));

        Mockito.when(request.checkNotModified("W/\"tag\"")).thenReturn(true);
        Assertions.assertTrue(controller.checkEtag(request, "match"));
    }

    @Test
    public void test_l10n_noInstance() {
        ResourceController controller = new TestController();

        ResponseEntity<PrimitiveMap<String>> result = controller.getL10n(null, (Locale) null, null);
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getBody());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void test_l10n_noPrefix() {
        L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected L10N getL10N() {
                return l10n;
            }
        };

        Mockito.when(l10n.hasLocale(Locale.ENGLISH)).thenReturn(true);
        Mockito.when(l10n.getPublicText(Locale.ENGLISH, null)).thenReturn(new HashMap<>());

        ResponseEntity<PrimitiveMap<String>> result = controller.getL10n(null, Locale.ENGLISH, null);
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getBody());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void test_l10n_noLocale() {
        L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected L10N getL10N() {
                return l10n;
            }
        };

        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(l10n.getPublicText(Locale.ENGLISH, "a")).thenReturn(new HashMap<>());

        ResponseEntity<PrimitiveMap<String>> result = controller.getL10n(null, (Locale) null, "a");
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getBody());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void test_l10n_localeNotSupported() {
        L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected L10N getL10N() {
                return l10n;
            }
        };

        Mockito.when(l10n.hasLocale(Locale.GERMAN)).thenReturn(false);
        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(l10n.getPublicText(Locale.ENGLISH, null)).thenReturn(new HashMap<>());

        ResponseEntity<PrimitiveMap<String>> result = controller.getL10n(null, Locale.GERMAN, null);
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getBody());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void test_l10n_hasValues() {
        L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected L10N getL10N() {
                return l10n;
            }
        };

        Mockito.when(l10n.hasLocale(Locale.ENGLISH)).thenReturn(true);
        Map<String, String> values = new HashMap<>();
        values.put("a", "One");
        Mockito.when(l10n.getPublicText(Locale.ENGLISH, "a")).thenReturn(values);

        ResponseEntity<PrimitiveMap<String>> result = controller.getL10n(null, Locale.ENGLISH, "a");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertEquals(1, result.getBody().size());
        Assertions.assertEquals("One", result.getBody().get("a"));
    }

    @Test
    public void test_l10n_notModified() {
        L10N l10n = Mockito.mock(L10N.class);
        ETagService etagService = Mockito.mock(ETagService.class);
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected L10N getL10N() {
                return l10n;
            }

            @Override
            protected ETagService getETagService() {
                return etagService;
            }
        };

        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(etagService.getETag("l10n/" + Locale.ENGLISH.toString() + "/a")).thenReturn("W/\"tag\"");
        Mockito.when(request.checkNotModified("W/\"tag\"")).thenReturn(true);

        ResponseEntity<PrimitiveMap<String>> result = controller.getL10n(request, Locale.ENGLISH, "a");
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getBody());
        Assertions.assertEquals(HttpStatus.NOT_MODIFIED, result.getStatusCode());
    }

    @Test
    public void test_l10n_byName_null() {
        L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected L10N getL10N() {
                return l10n;
            }
        };

        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(l10n.getPublicText(Locale.ENGLISH, null)).thenReturn(new HashMap<>());

        ResponseEntity<PrimitiveMap<String>> result = controller.l10n(null, (String) null, null);
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getBody());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void test_l10n_byName_empty() {
        L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected L10N getL10N() {
                return l10n;
            }
        };

        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(l10n.getPublicText(Locale.ENGLISH, null)).thenReturn(new HashMap<>());

        ResponseEntity<PrimitiveMap<String>> result = controller.l10n(null, "", null);
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getBody());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void test_l10n_byName_blank() {
        L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected L10N getL10N() {
                return l10n;
            }
        };

        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(l10n.getPublicText(Locale.ENGLISH, null)).thenReturn(new HashMap<>());

        ResponseEntity<PrimitiveMap<String>> result = controller.l10n(null, " \t \r \n \t ", null);
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getBody());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void test_l10n_byName() {
        L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected L10N getL10N() {
                return l10n;
            }
        };

        Mockito.when(l10n.hasLocale(Locale.ENGLISH)).thenReturn(true);
        Map<String, String> map = new HashMap<>();
        map.put("a", "A");
        Mockito.when(l10n.getPublicText(Locale.ENGLISH, "a")).thenReturn(map);

        ResponseEntity<PrimitiveMap<String>> result = controller.l10n(null, Locale.ENGLISH.toLanguageTag(), "a");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals("A", result.getBody().get("a"));
    }

    @Test
    public void test_l10n_byName_junk() {
        L10N l10n = Mockito.mock(L10N.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected L10N getL10N() {
                return l10n;
            }
        };

        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Map<String, String> map = new HashMap<>();
        map.put("a", "A");
        Mockito.when(l10n.getPublicText(Locale.ENGLISH, "a")).thenReturn(map);

        ResponseEntity<PrimitiveMap<String>> result = controller.l10n(null, "not_a_locale_code", "a");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals("A", result.getBody().get("a"));
    }

    @Test
    public void test_response_jsonableStringMap_noEtagService() {
        ResourceController controller = new TestController();

        PrimitiveMap<String> map = new PrimitiveMap<>();
        ResponseEntity<PrimitiveMap<String>> result = controller.respond(map, "key");
        Assertions.assertNotNull(result);
        Assertions.assertSame(map, result.getBody());
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNull(result.getHeaders().getCacheControl());
        Assertions.assertNull(result.getHeaders().getETag());
    }

    @Test
    public void test_response_jsonableStringMap() {
        ETagService etagService = Mockito.mock(ETagService.class);
        ResourceController controller = new ResourceController() {
            @Override
            protected ETagService getETagService() {
                return etagService;
            }
        };

        Mockito.when(etagService.getETag("key")).thenReturn("W/\"tag\"");

        PrimitiveMap<String> map = new PrimitiveMap<>();
        ResponseEntity<PrimitiveMap<String>> result = controller.respond(map, "key");
        Assertions.assertNotNull(result);
        Assertions.assertSame(map, result.getBody());
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"tag\"", result.getHeaders().getETag());
    }

    @Test
    public void test_response_InputStreamResource_noEtagService() {
        ResourceController controller = new TestController();

        InputStreamResource content = Mockito.mock(InputStreamResource.class);

        ResponseEntity<InputStreamResource> result = controller.respond(content, "svg", "key");
        Assertions.assertNotNull(result);
        Assertions.assertSame(content, result.getBody());
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNull(result.getHeaders().getCacheControl());
        Assertions.assertNull(result.getHeaders().getETag());
        Assertions.assertNull(result.getHeaders().get(HttpHeaders.CONTENT_ENCODING));
    }

    @Test
    public void test_response_InputStreamResource_noEtagService_svgz() {
        ResourceController controller = new TestController();

        InputStreamResource content = Mockito.mock(InputStreamResource.class);

        ResponseEntity<InputStreamResource> result = controller.respond(content, "svgz", "key");
        Assertions.assertNotNull(result);
        Assertions.assertSame(content, result.getBody());
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNull(result.getHeaders().getCacheControl());
        Assertions.assertNull(result.getHeaders().getETag());
        Assertions.assertEquals(Arrays.asList("gzip"), result.getHeaders().get(HttpHeaders.CONTENT_ENCODING));
    }

    @Test
    public void test_response_InputStreamResource() {
        ETagService etagService = Mockito.mock(ETagService.class);
        ResourceController controller = new ResourceController() {
            @Override
            protected ETagService getETagService() {
                return etagService;
            }
        };

        Mockito.when(etagService.getETag("key")).thenReturn("W/\"tag\"");

        InputStreamResource content = new InputStreamResource(new ByteArrayInputStream(new byte[0]));
        ResponseEntity<InputStreamResource> result = controller.respond(content, "jpg", "key");
        Assertions.assertNotNull(result);
        Assertions.assertSame(content, result.getBody());
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"tag\"", result.getHeaders().getETag());
        Assertions.assertNull(result.getHeaders().get(HttpHeaders.CONTENT_ENCODING));
    }

    @Test
    public void test_response_InputStreamResource_unsupportedMediaType() {
        ETagService etagService = Mockito.mock(ETagService.class);
        ResourceController controller = new ResourceController() {
            @Override
            protected ETagService getETagService() {
                return etagService;
            }
        };

        Mockito.when(etagService.getETag("key")).thenReturn("W/\"tag\"");

        InputStreamResource content = new InputStreamResource(new ByteArrayInputStream(new byte[0]));
        ResponseEntity<InputStreamResource> result = controller.respond(content, "junk", "key");
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getBody());
        Assertions.assertEquals(HttpStatus.NOT_ACCEPTABLE, result.getStatusCode());
    }

    @Test
    public void test_getPageScript_nullAllowedPages() {
        ResourceController controller = new TestController();

        ResponseEntity<String> result = controller.getPageScript(null, "index");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getPageScript_emptyAllowedPages() {
        ResourceController controller = new ResourceController() {
            @Override
            protected Set<String> getAllowedPages() {
                return new HashSet<>();
            }
        };

        ResponseEntity<String> result = controller.getPageScript(null, "index");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getPageScript_pageNotAllowed() {
        ResourceController controller = new ResourceController() {
            @Override
            protected Set<String> getAllowedPages() {
                return new HashSet<>(Arrays.asList("/home"));
            }
        };

        ResponseEntity<String> result = controller.getPageScript(null, "index");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getPageScript_nonCahing() {
        final WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected Set<String> getAllowedPages() {
                return new HashSet<>(Arrays.asList("index"));
            }

            @Override
            protected boolean isCaching() {
                return false;
            }
        };

        String testJs = new String(IoTools.readFile("static-library/js-templates" + "/lib-page.js"), StandardCharsets.UTF_8);
        Assertions.assertNotNull(testJs);
        testJs = testJs.replace("%Page%", "index");

        ResponseEntity<String> result = controller.getPageScript(request, "index");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertEquals(testJs, result.getBody());
        Assertions.assertNull(result.getHeaders().getCacheControl());
        Assertions.assertNull(result.getHeaders().getETag());
        final String firstBody = result.getBody();

        result = controller.getPageScript(request, "index");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertEquals(testJs, result.getBody());
        Assertions.assertNull(result.getHeaders().getCacheControl());
        Assertions.assertNull(result.getHeaders().getETag());
        Assertions.assertEquals(firstBody, result.getBody());
        Assertions.assertNotSame(firstBody, result.getBody());
    }

    @Test
    public void test_getPageScript_cahing_hasModified() {
        final ETagService etagService = Mockito.mock(ETagService.class);
        final WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected Set<String> getAllowedPages() {
                return new HashSet<>(Arrays.asList("index"));
            }

            @Override
            protected boolean isCaching() {
                return true;
            }

            @Override
            protected ETagService getETagService() {
                return etagService;
            }
        };

        Mockito.when(etagService.getETag("-js-index")).thenReturn("W/\"etag\"");
        Mockito.when(request.checkNotModified("W/\"etag\"")).thenReturn(false);

        String testJs = new String(IoTools.readFile("static-library/js-templates" + "/lib-page.js"), StandardCharsets.UTF_8);
        Assertions.assertNotNull(testJs);
        testJs = testJs.replace("%Page%", "index");

        ResponseEntity<String> result = controller.jsPage(request, "index");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertEquals(testJs, result.getBody());
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        final String firstBody = result.getBody();

        result = controller.jsPage(request, "index");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertEquals(testJs, result.getBody());
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"etag\"", result.getHeaders().getETag());
        Assertions.assertEquals(firstBody, result.getBody());
        Assertions.assertNotSame(firstBody, result.getBody());
    }

    @Test
    public void test_getPageScript_cahing_notModified() {
        final ETagService etagService = Mockito.mock(ETagService.class);
        final WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected Set<String> getAllowedPages() {
                return new HashSet<>(Arrays.asList("index"));
            }

            @Override
            protected boolean isCaching() {
                return true;
            }

            @Override
            protected ETagService getETagService() {
                return etagService;
            }

            @Override
            protected String getCacheKeyPrefix() {
                return "cache";
            }
        };

        Mockito.when(etagService.getETag("cache-js-index")).thenReturn("W/\"etag\"");
        Mockito.when(request.checkNotModified("W/\"etag\"")).thenReturn(true);

        ResponseEntity<String> result = controller.getPageScript(request, "index");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_MODIFIED, result.getStatusCode());
        Assertions.assertNull(result.getBody());
        Assertions.assertNull(result.getHeaders().getCacheControl());
        Assertions.assertNull(result.getHeaders().getETag());
    }

    @Test
    public void test_favicon() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new TestController();

        ResponseEntity<InputStreamResource> result = controller.favicon(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getStreamResource_nullRequest() {
        ResourceController controller = new TestController();

        ResponseEntity<InputStreamResource> result = controller.getStreamResource(Types.img, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getStreamResource_nullPath() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new TestController();

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)).thenReturn(null);

        ResponseEntity<InputStreamResource> result = controller.getStreamResource(Types.img, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getStreamResource_badPath() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new TestController();

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .thenReturn("./img/../img.jpg");

        ResponseEntity<InputStreamResource> result = controller.getStreamResource(Types.img, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getStreamResource_notImage() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected List<StaticResourceFileResolver> getFileResolvers() {
                return Arrays.asList(new StaticResourceFileResolver("static-app", 1));
            }
        };

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)).thenReturn("img/black.txt");

        ResponseEntity<InputStreamResource> result = controller.getStreamResource(Types.img, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getStreamResource_notConfigured() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController();

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)).thenReturn("img/random.png");

        ResponseEntity<InputStreamResource> result = controller.getStreamResource(Types.img, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getStreamResource_notFound() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController();

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)).thenReturn("img/random.png");

        ResponseEntity<InputStreamResource> result = controller.getStreamResource(Types.img, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getStreamResource_external_jpg_noCaching() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected List<StaticResourceFileResolver> getFileResolvers() {
                return Arrays.asList(new StaticResourceFileResolver("static-ext", 2));
            }
        };

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)).thenReturn("img/black.jpg");

        ResponseEntity<InputStreamResource> result = controller.getStreamResource(Types.img, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
    }

    @Test
    public void test_img_jpg_noCaching() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected List<StaticResourceFileResolver> getFileResolvers() {
                return Arrays.asList(new StaticResourceFileResolver("static-app", 2));
            }
        };

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)).thenReturn("img/black.jpg");

        ResponseEntity<InputStreamResource> result = controller.img(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
    }

    @Test
    public void test_getStreamResource_svg_caching_notCached() {
        WebRequest request = Mockito.mock(WebRequest.class);
        ETagService etagService = Mockito.mock(ETagService.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected List<StaticResourceFileResolver> getFileResolvers() {
                return Arrays.asList(new StaticResourceFileResolver("static-app", 2));
            }

            @Override
            protected ETagService getETagService() {
                return etagService;
            }

            @Override
            protected String getCacheKeyPrefix() {
                return "prefix";
            }
        };

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)).thenReturn("img/text.svgz");
        Mockito.when(etagService.getETag("prefix-img-img/text.svgz")).thenReturn("W/\"tag\"");
        Mockito.when(request.checkNotModified("W/\"tag\"")).thenReturn(false);

        ResponseEntity<InputStreamResource> result = controller.getStreamResource(Types.img, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"tag\"", result.getHeaders().getETag());
    }

    @Test
    public void test_getStreamResource_svg_caching_cached() {
        WebRequest request = Mockito.mock(WebRequest.class);
        ETagService etagService = Mockito.mock(ETagService.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected ETagService getETagService() {
                return etagService;
            }

            @Override
            protected String getCacheKeyPrefix() {
                return "prefix";
            }
        };

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)).thenReturn("img/text.svgz");
        Mockito.when(etagService.getETag("prefix-img-img/text.svgz")).thenReturn("W/\"tag\"");
        Mockito.when(request.checkNotModified("W/\"tag\"")).thenReturn(true);

        ResponseEntity<InputStreamResource> result = controller.getStreamResource(Types.img, request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_MODIFIED, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_font() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController();

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .thenReturn("font/fontawesome/fa-regular-400.woff2");

        ResponseEntity<InputStreamResource> result = controller.font(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
    }

    @Test
    public void test_font_notFound() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController();

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .thenReturn("font/fontawesome/fa-regular-400.woff");

        ResponseEntity<InputStreamResource> result = controller.font(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_font_notFound_v2() {
        WebRequest request = Mockito.mock(WebRequest.class);

        ResourceController controller = new ResourceController() {
            @Override
            protected List<StaticResourceFileResolver> getFileResolvers() {
                return new ArrayList<>();
            }
        };

        Mockito.when(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .thenReturn("font/fontawesome/fa-regular-400.woff2");

        ResponseEntity<InputStreamResource> result = controller.font(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_getCsrfTokenHeaderId() {
        ResourceController controller = new TestController();
        Assertions.assertNull(controller.getCsrfTokenHeaderId());

        CsrfTokenManager csrfTokenManager = Mockito.mock(CsrfTokenManager.class);
        this.setField("csrfTokenManager", csrfTokenManager, controller);

        Mockito.when(csrfTokenManager.getHeaderId()).thenReturn("X-CSRF");
        Assertions.assertEquals("X-CSRF", controller.getCsrfTokenHeaderId());
    }

    @Test
    public void test_getFileResolvers() {
        ResourceController controller = new ResourceController();

        List<StaticResourceFileResolver> result = controller.getFileResolvers();
        Assertions.assertSame(result, controller.getFileResolvers());
    }

    @Test
    public void test_getFileResolvers_withThemes() {
        ResourceController controller = new ResourceController();

        ThemeResourceFileResolver libThemeResolver = new ThemeResourceFileResolver("root", 1);
        ThemeResourceFileResolver appThemeResolver = new ThemeResourceFileResolver("", 1);
        ThemeResourceFileResolver extThemeResolver = new ThemeResourceFileResolver(null, 1);
        this.setField("libThemeResolver", libThemeResolver, controller);
        this.setField("appThemeResolver", appThemeResolver, controller);
        this.setField("extThemeResolver", extThemeResolver, controller);

        List<StaticResourceFileResolver> result = controller.getFileResolvers();
        Assertions.assertNotNull(result);
        for (StaticResourceFileResolver resolver : result) {
            if (resolver instanceof ThemeResourceFileResolver) {
                String root = ((ThemeResourceFileResolver) resolver).getThemesRoot();
                Assertions.assertNotNull(root);
                Assertions.assertNotEquals("", root);
            }
        }
    }

    @Test
    public void test_getFileResolversReverse() {
        ResourceController controller = new ResourceController() {
            @Override
            protected List<StaticResourceFileResolver> getFileResolvers() {
                return new ArrayList<>();
            }
        };

        List<StaticResourceFileResolver> result = controller.getFileResolversReverse();
        Assertions.assertTrue(result.isEmpty());
        Assertions.assertSame(result, controller.getFileResolversReverse());
    }
}
