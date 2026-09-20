package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.web.http.receptionist.sse.ServerSentEvent;
import io.vertx.core.Future;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;
import org.jspecify.annotations.NullMarked;

/**
 * Lightweight base for SSE endpoints. Subclasses implement {@link #handle()} and own
 * the response lifecycle: validation, headers, ordered writes, end/reset and upstream cleanup.
 * Declare {@code timeout=0} on {@link ApiMeta} for long-lived routes.
 * No automatic error event, heartbeat, timeout or disconnect handler is installed.
 *
 * @since 5.0.4
 */
@NullMarked
public abstract class KeelWebSseReceptionist extends KeelWebReceptionist {
    public KeelWebSseReceptionist(RoutingContext context) {
        super(context);
    }

    /**
     * Configures SSE headers and HTTP/1.1 chunking before the first write.
     * This does not send or end the response. HTTP/2 framing is handled by Vert.x.
     *
     * @param routingContext request whose response will carry SSE output
     */
    public static void setHeadersForSSE(RoutingContext routingContext) {
        routingContext.response().putHeader("Content-Type", "text/event-stream; charset=utf-8")
                      .putHeader("Cache-Control", "no-cache, no-transform")
                      .putHeader("X-Accel-Buffering", "no");
        if (routingContext.request().version() == HttpVersion.HTTP_1_1) {
            routingContext.response().setChunked(true);
        }
    }

    /**
     * Encodes and writes one event or comment. Chain calls with {@link Future#compose}
     * to order output, then explicitly end the response when production finishes.
     * Configure headers with {@link #setHeadersForSSE(RoutingContext)} first.
     *
     * @param event event to encode; must not be modified concurrently with this call
     * @return a Future tracking the write, including encoding and synchronous write failures;
     *         success is not client acknowledgement and adds no drain waiting
     * @implNote Failure does not automatically end/reset the response or cancel the producer.
     */
    protected Future<Void> pushOneEvent(ServerSentEvent event) {
        return Future.succeededFuture()
                .compose(v -> getRoutingContext().response().write(event.encode()));
    }
}
