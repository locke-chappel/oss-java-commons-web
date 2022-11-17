package com.github.lc.oss.commons.web.controllers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerMapping;

import com.github.lc.oss.commons.l10n.L10N;
import com.github.lc.oss.commons.l10n.UserLocale;
import com.github.lc.oss.commons.serialization.PrimitiveMap;
import com.github.lc.oss.commons.util.IoTools;
import com.github.lc.oss.commons.web.resources.AbstractResourceResolver;
import com.github.lc.oss.commons.web.resources.Minifier;
import com.github.lc.oss.commons.web.resources.StaticResourceFileResolver;
import com.github.lc.oss.commons.web.services.ETagService;
import com.github.lc.oss.commons.web.tokens.CsrfTokenManager;
import com.github.lc.oss.commons.web.util.ContextUtil;

public class ResourceController extends AbstractResourceResolver {
    protected static final Map<String, String> FILE_EXT_MEDIATYPES;
    static {
        Map<String, String> mediaMap = new HashMap<>();
        mediaMap.put("gif", "image/gif");
        mediaMap.put("ico", "image/x-icon");
        mediaMap.put("jpg", "image/jpeg");
        mediaMap.put("jpeg", "image/jpeg");
        mediaMap.put("png", "image/png");
        mediaMap.put("svg", "image/svg+xml");
        mediaMap.put("svgz", "image/svg+xml");
        mediaMap.put("woff2", "font/woff2");
        FILE_EXT_MEDIATYPES = Collections.unmodifiableMap(mediaMap);
    }

    private final Map<String, String> cache = new HashMap<>();

    private List<StaticResourceFileResolver> reverseResolvers;

    @Autowired(required = false)
    private L10N l10n;
    @Autowired(required = false)
    private ServletContext context;
    @Autowired(required = false)
    private UserLocale userLocale;
    @Autowired(required = false)
    private Minifier minifier;
    @Autowired(required = false)
    private CsrfTokenManager csrfTokenManager;
    @Autowired(required = false)
    private ETagService etagService;
    @Autowired(required = false)
    @Qualifier("libThemeResourceFileResolver")
    private ThemeResourceFileResolver libThemeResolver;
    @Autowired(required = false)
    @Qualifier("appThemeResourceFileResolver")
    private ThemeResourceFileResolver appThemeResolver;
    @Autowired(required = false)
    @Qualifier("extThemeResourceFileResolver")
    private ThemeResourceFileResolver extThemeResolver;
    @Autowired(required = false)
    private UserTheme userTheme;

    @Value("${application.ui.caching:true}")
    private boolean enableCaching;
    @Value("${application.ui.logging.prefix:}")
    private String consoleLogPrefix;
    @Value("#{pathNormalizer.dir('${application.ui.external-path:}')}")
    private String externalResourcePath;
    @Value("#{pathNormalizer.dir('${application.ui.resource-path:static-secure/}')}")
    private String appResourcePath;
    @Value("${application.ui.research.search-depth:5}")
    private int searchDepth;

    protected void clearCache() {
        this.cache.clear();
    }

    protected Set<String> getAllowedPages() {
        return null;
    }

    protected String getAppResourcePath() {
        return this.appResourcePath;
    }

    protected String getCacheKeyPrefix() {
        return null;
    }

    @Override
    protected String getConsoleLogPrefix() {
        return this.consoleLogPrefix;
    }

    @Override
    protected String getContextPath() {
        if (this.context == null) {
            return null;
        }

        return ContextUtil.getAbsoluteUrl("/", this.context);
    }

    @Override
    protected String getCsrfTokenHeaderId() {
        if (this.csrfTokenManager == null) {
            return null;
        }

        return this.csrfTokenManager.getHeaderId();
    }

    protected ETagService getETagService() {
        return this.etagService;
    }

    protected String getExternalResourcePath() {
        return this.externalResourcePath;
    }

    @Override
    protected List<StaticResourceFileResolver> getFileResolvers() {
        if (this.resolvers == null) {
            List<StaticResourceFileResolver> resolvers = Arrays.asList( //
                    AbstractResourceResolver.LIBRARY_RESOLVER, //
                    this.libThemeResolver, //
                    new StaticResourceFileResolver(this.getAppResourcePath(), this.getSearchDepth()), //
                    this.appThemeResolver, //
                    new StaticResourceFileResolver(this.getExternalResourcePath(), this.getSearchDepth()), //
                    this.extThemeResolver);

            this.resolvers = resolvers.stream(). //
                    filter(r -> r != null). //
                    filter(r -> {
                        if (r instanceof ThemeResourceFileResolver) {
                            String themesRoot = ((ThemeResourceFileResolver) r).getThemesRoot();
                            if (themesRoot == null) {
                                return false;
                            }
                        }
                        return true;
                    }). //
                    collect(Collectors.toUnmodifiableList());
        }
        return this.resolvers;

    }

    protected List<StaticResourceFileResolver> getFileResolversReverse() {
        if (this.reverseResolvers == null) {
            List<StaticResourceFileResolver> resolvers = this.getFileResolvers();
            List<StaticResourceFileResolver> reverse = new ArrayList<>();
            if (!resolvers.isEmpty()) {
                for (int i = resolvers.size() - 1; i >= 0; i--) {
                    reverse.add(resolvers.get(i));
                }
            }
            this.reverseResolvers = Collections.unmodifiableList(reverse);
        }
        return this.reverseResolvers;
    }

    @Override
    protected L10N getL10N() {
        return this.l10n;
    }

    protected int getSearchDepth() {
        return this.searchDepth;
    }

    @Override
    protected UserLocale getUserLocale() {
        return this.userLocale;
    }

    protected Minifier getMinifier() {
        return this.minifier;
    }

    protected UserTheme getUserTheme() {
        return this.userTheme;
    }

    protected boolean isCaching() {
        return this.enableCaching;
    }

    @GetMapping(path = "/css", produces = "text/css")
    public ResponseEntity<String> css(WebRequest request) {
        return this.get(Types.css, request);
    }

    @GetMapping(path = "/favicon.ico")
    public ResponseEntity<InputStreamResource> favicon(WebRequest request) {
        return this.getStreamResource(Types.img, request);
    }

    @GetMapping(path = "/font/**")
    public ResponseEntity<InputStreamResource> font(WebRequest request) {
        return this.getStreamResource(Types.font, request);
    }

    @GetMapping(path = "/img/**")
    public ResponseEntity<InputStreamResource> img(WebRequest request) {
        return this.getStreamResource(Types.img, request);
    }

    @GetMapping(path = "/js", produces = "text/javascript")
    public ResponseEntity<String> js(WebRequest request) {
        return this.get(Types.js, request);
    }

    @GetMapping(path = "/js/{page}", produces = "text/javascript")
    public ResponseEntity<String> jsPage(WebRequest request, @PathVariable("page") String page) {
        return this.getPageScript(request, page);
    }

    @GetMapping(path = "/l10n/{locale}/{prefix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrimitiveMap<String>> l10n(WebRequest request, @PathVariable("locale") String locale, @PathVariable("prefix") String prefix) {
        return this.getL10n(request, locale, prefix);
    }

    protected boolean checkEtag(WebRequest request, String etagId) {
        if (request == null || this.getETagService() == null) {
            return false;
        }

        String etag = this.getETagService().getETag(etagId);
        return request.checkNotModified(etag);
    }

    protected ResponseEntity<String> get(Types type) {
        return this.get(type, null);
    }

    protected ResponseEntity<String> get(Types type, WebRequest request) {
        if (type == null) {
            throw new RuntimeException("Type cannot be null");
        }

        switch (type) {
            case css:
            case js:
                /* valid, nothing to do */
                break;
            default:
                throw new RuntimeException("This version of get() only supports CSS and JavaScript.");
        }

        String prefix = this.getCacheKeyPrefix();
        if (prefix == null) {
            prefix = "";
        }
        String theme = this.getUserTheme() == null ? null : this.getUserTheme().getName();
        if (theme != null) {
            prefix += theme;
        }
        String cacheKey = prefix + "-" + type.name();

        if (this.checkEtag(request, cacheKey)) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }

        String content = this.getFromCache(cacheKey);
        if (content == null) {
            content = this.compile(type);
            content = this.replaceValues(type, content);
            content = this.minify(type, content);
            if (this.isCaching()) {
                this.putInCache(cacheKey, content);
            }
        }

        return this.respond(content, cacheKey);
    }

    protected ResponseEntity<InputStreamResource> getStreamResource(Types type, WebRequest request) {
        if (request == null) {
            return this.notFound();
        }

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (path == null || path.contains("..")) {
            return this.notFound();
        }

        Matcher matcher = type.getFileExtensionsPattern().matcher(path);
        if (!matcher.matches()) {
            return this.notFound();
        }

        String prefix = this.getCacheKeyPrefix();
        if (prefix == null) {
            prefix = "";
        }

        String cacheKey = prefix + "-" + type.name() + "-" + path;
        if (this.checkEtag(request, cacheKey)) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }

        List<StaticResourceFileResolver> resolvers = this.getFileResolversReverse();
        List<String> files = null;
        for (StaticResourceFileResolver resolver : resolvers) {
            files = resolver.findFiles(type, p -> p.toString().replace("\\", "/").endsWith(path));
            if (files != null && files.size() == 1) {
                break;
            }
        }

        if (files == null || files.size() != 1) {
            return this.notFound();
        }

        return this.respond(new InputStreamResource(IoTools.readAbsoluteStream(files.iterator().next())), matcher.group(1), cacheKey);
    }

    protected String getFromCache(String key) {
        return this.cache.get(key);
    }

    protected ResponseEntity<String> getPageScript(WebRequest request, String page) {
        Set<String> allowed = this.getAllowedPages();
        if (allowed == null || !allowed.contains(page)) {
            return this.notFound();
        }

        String prefix = this.getCacheKeyPrefix();
        if (prefix == null) {
            prefix = "";
        }

        String cacheKey = prefix + "-js-" + page;
        if (this.checkEtag(request, cacheKey)) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }

        String script = this.getFromCache(cacheKey);
        if (script == null) {
            List<String> scripts = IoTools.listDir( //
                    AbstractResourceResolver.LIBRARY_PATH + "js-templates/", //
                    1, //
                    path -> path.toString().endsWith("lib-page.js"));
            byte[] bytes = IoTools.readAbsoluteFile(scripts.iterator().next());
            script = new String(bytes, StandardCharsets.UTF_8);

            if (this.isCaching()) {
                this.putInCache(cacheKey, script);
            }
        }
        script = this.replaceValues(Types.js, script);
        script = script.replace("%Page%", page);
        return this.respond(script, cacheKey);
    }

    protected ResponseEntity<PrimitiveMap<String>> getL10n(WebRequest request, String localeName, String prefix) {
        Locale locale = null;
        if (localeName != null && !localeName.trim().equals("")) {
            locale = new Locale(localeName.replace("-", "_"));
        }
        return this.getL10n(request, locale, prefix);
    }

    protected ResponseEntity<PrimitiveMap<String>> getL10n(WebRequest request, Locale locale, String prefix) {
        if (this.getL10N() == null) {
            return this.notFound();
        }

        if (locale == null || !this.getL10N().hasLocale(locale)) {
            locale = this.getL10N().getDefaultLocale();
        }

        String key = "l10n/" + locale.toString() + "/" + prefix;
        if (this.checkEtag(request, key)) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }

        Map<String, String> values = this.getL10N().getPublicText(locale, prefix);
        if (values.isEmpty()) {
            return this.notFound();
        }

        return this.respond(new PrimitiveMap<>(values), key);
    }

    protected String minify(Types type, String content) {
        Minifier minifier = this.getMinifier();
        if (minifier == null) {
            return content;
        }

        switch (type) {
            case css:
                return minifier.minifyCssIfEnabled(content);
            case js:
                return minifier.minifyJsIfEnabled(content);
            default:
                return content;
        }
    }

    protected <T> ResponseEntity<T> notFound() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    protected String putInCache(String key, String value) {
        return this.cache.put(key, value);
    }

    protected ResponseEntity<String> respond(String content, String etagId) {
        HttpHeaders headers = null;
        ETagService eTagService = this.getETagService();
        if (eTagService != null) {
            headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noCache().cachePrivate());
            headers.setETag(eTagService.getETag(etagId));
        }
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    protected ResponseEntity<PrimitiveMap<String>> respond(PrimitiveMap<String> content, String etagId) {
        HttpHeaders headers = null;
        ETagService eTagService = this.getETagService();
        if (eTagService != null) {
            headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noCache().cachePrivate());
            headers.setETag(eTagService.getETag(etagId));
        }
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    protected ResponseEntity<InputStreamResource> respond(InputStreamResource content, String fileType, String etagId) {
        HttpHeaders responseHeaders = new HttpHeaders();
        ETagService eTagService = this.getETagService();
        if (eTagService != null) {
            responseHeaders.set(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());
            responseHeaders.set(HttpHeaders.ETAG, eTagService.getETag(etagId));
        }

        String mediaType = ResourceController.FILE_EXT_MEDIATYPES.get(fileType);
        if (mediaType == null) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, mediaType);

        if (fileType.equals("svgz")) {
            responseHeaders.set(HttpHeaders.CONTENT_ENCODING, "gzip");
        }

        return new ResponseEntity<>(content, responseHeaders, HttpStatus.OK);
    }
}
