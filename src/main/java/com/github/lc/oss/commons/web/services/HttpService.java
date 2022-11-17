package com.github.lc.oss.commons.web.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

public class HttpService {
    private static final StringHttpMessageConverter UTF_8_CONVERTER = new StringHttpMessageConverter(StandardCharsets.UTF_8);
    private static final RedirectStrategy REDIRECT_STRATEGY = new DefaultRedirectStrategy(new String[0]);

    protected static final int DEFAULT_TIMEOUT = 30 * 1000;

    public <T> ResponseEntity<T> call(HttpMethod method, String url, Map<String, String> headers, Class<T> responseType, Object body) {
        HttpHeaders requestHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach((k, v) -> requestHeaders.add(k, v));
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Invalid URL", ex);
        }

        return this.createRestTemplate().exchange(uri, method, new HttpEntity<>(body, requestHeaders), responseType);
    }

    protected ClientHttpRequestFactory createRequestFactory() {
        /*
         * Long standing Java bug - PATCH isn't supported by default :(
         *
         * Note: HttpClient is not reusable :(
         */
        HttpClient client = HttpClientBuilder.create(). //
                setRedirectStrategy(HttpService.REDIRECT_STRATEGY). //
                build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
        factory.setConnectTimeout(this.getTimeout());
        factory.setReadTimeout(this.getTimeout());
        return factory;
    }

    protected RestTemplate createRestTemplate() {
        RestTemplate rest = new RestTemplate();
        ClientHttpRequestFactory factory = this.createRequestFactory();
        if (factory != null) {
            rest.setRequestFactory(factory);
        }

        /*
         * Spring 5.2+ "bug" - encoding headers are no longer supplied so JSON strings
         * get ISO_8859_1 instead of UTF-8 - which is a bug since JSON is UTF-8...
         */
        rest.getMessageConverters().add(0, HttpService.UTF_8_CONVERTER);
        return rest;
    }

    public int getTimeout() {
        return HttpService.DEFAULT_TIMEOUT;
    }
}
