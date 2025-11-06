package io.github.sinri.keel.web.http.requester;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.function.Function;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

/**
 *
 * @since 4.0.3
 * @deprecated This should be a philósophy, not be a fixed implementation.
 */
@Deprecated(since = "4.1.5")
public interface KeelWebRequestMixin {

    /**
     * @since 4.0.1
     */
    default <T> Future<T> useWebClient(
            WebClientOptions webClientOptions,
            Function<WebClient, Future<T>> usage
    ) {
        WebClient webClient = WebClient.create(Keel.getVertx(), webClientOptions);
        return Future.succeededFuture()
                     .compose(v -> usage.apply(webClient))
                     .onComplete(ar -> webClient.close());
    }

    /**
     * @since 3.2.18
     * @since 3.2.19 Fix to avoid cross-verticle loss.
     */
    default <T> Future<T> useWebClient(Function<WebClient, Future<T>> usage) {
        return useWebClient(new WebClientOptions(), usage);
    }

    /**
     * @since 4.0.1
     */
    default <T> Future<T> useHttpClient(HttpClientOptions httpClientOptions, Function<HttpClient, Future<T>> usage) {
        HttpClient httpClient = Keel.getVertx().createHttpClient(httpClientOptions);
        return Future.succeededFuture()
                     .compose(v -> usage.apply(httpClient))
                     .onComplete(ar -> httpClient.close());
    }
}
