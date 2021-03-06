package tech.kronicle.gradlestaticanalyzer.internal.services;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

import java.net.http.HttpResponse;
import java.util.function.Function;

public class HttpRequestMaker {

    private static final String RETRY_NAME = "http-request-maker";

    private final Retry retry;

    public HttpRequestMaker(RetryRegistry retryRegistry) {
        this.retry = retryRegistry.retry(RETRY_NAME, RETRY_NAME);
    }

    public HttpResponse<String> makeHttpRequest(Function<String, HttpResponse<String>> httpRequest, String url) {
        return retry.executeSupplier(() -> httpRequest.apply(url));
    }
}
