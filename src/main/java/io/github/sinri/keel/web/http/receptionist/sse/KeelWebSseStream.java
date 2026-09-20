package io.github.sinri.keel.web.http.receptionist.sse;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;
import org.jspecify.annotations.NullMarked;

/**
 * Caller-ordered SSE output. Chain sends with compose and await the last send before end.
 * Concurrent operations are unsupported. No internal queue, timer or automatic heartbeat.
 *
 * @since 5.0.4
 */
@Deprecated
@NullMarked
public interface KeelWebSseStream {

    static void setHeaders(RoutingContext routingContext) {
        routingContext.response().putHeader("Content-Type", "text/event-stream; charset=utf-8")
                      .putHeader("Cache-Control", "no-cache, no-transform")
                      .putHeader("X-Accel-Buffering", "no");
        if (routingContext.request().version() == HttpVersion.HTTP_1_1) {
            routingContext.response().setChunked(true);
        }
    }

    static KeelWebSseStream createDefaultInstance(HttpServerResponse response) {
        return new DefaultKeelWebSseStream(response);
    }

    /**
     * Returns the response write Future. No additional drain waiting or client acknowledgement.
     */
    Future<Void> sendEvent(ServerSentEvent event);

    /**
     * Sends a comment, e.g. a heartbeat, through the same caller-controlled send chain.
     */
    Future<Void> sendComment(String comment);

    /**
     * Idempotently ends the response. Call only after the last send has completed.
     */
    Future<Void> end();

    /**
     * Success on normal end, failure on disconnect or output failure. Use for cleanup.
     */
    Future<Void> completion();

}
