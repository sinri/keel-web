package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.web.http.receptionist.sse.ServerSentEvent;
import io.github.sinri.keel.web.http.receptionist.sse.KeelWebSseStream;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.jspecify.annotations.NullMarked;

/**
 * SSE receptionist. Declare timeout=0 on ApiMeta for long-lived routes.
 * The stream owns the response; subclasses must not write to it directly.
 * @since 5.0.4
 */
@NullMarked
public abstract class KeelWebSseReceptionist extends KeelWebReceptionist {
    public KeelWebSseReceptionist(RoutingContext context) {
        super(context);
    }

    /** Prepares the request before streaming. The business controls timeouts and cancellation. */
    protected Future<Void> prepare() {
        return Future.succeededFuture();
    }

    /**
     * Completes when production ends. Register cancellation through stream.completion().
     */
    protected abstract Future<Void> handleStream(KeelWebSseStream stream);

    @Override
    public final void handle() {
        Future.succeededFuture()
              .compose(v -> prepare())
              .compose(v -> respondWithStream())
              .onFailure(error -> {
                  var response = getRoutingContext().response();
                  if (!response.closed() && !response.ended()) {
                      if (response.headWritten()) {
                          response.reset().onFailure(resetError -> getLogger().error(log -> log
                                  .message("SSE reset failed").exception(resetError)));
                      } else {
                          getRoutingContext().fail(error);
                      }
                  }
              });
    }

    private Future<Void> sendResponseHeaders() {
        KeelWebSseStream.setHeaders(getRoutingContext());
        return Future.succeededFuture();
    }

    private Future<Void> respondWithStream() {
        var response = getRoutingContext().response();
        if (response.closed() || response.ended()) return Future.succeededFuture();

        return Future.succeededFuture()
                .compose(v -> sendResponseHeaders())
                .compose(v -> {
                    var stream = KeelWebSseStream.createDefaultInstance(response);
                    return Future.succeededFuture()
                            .compose(ignored -> handleStream(stream))
                            .recover(error -> {
                                // Failed writes already terminate the stream; do not send another event.
                                if (stream.completion().failed()) return Future.failedFuture(error);
                                return sendStreamError(stream, error);
                            })
                            .eventually(stream::end)
                            .recover(error -> {
                                if (!stream.completion().failed()) return Future.failedFuture(error);
                                getLogger().error(log -> log.message("SSE transport failed").exception(error));
                                return Future.succeededFuture();
                            });
                });
    }

    private Future<Void> sendStreamError(KeelWebSseStream stream, Throwable error) {
        getLogger().error(log -> log.message("SSE production failed").exception(error));
        return stream.sendEvent(new ServerSentEvent()
                .event("error")
                .data(new JsonObject()
                        .put("code", "FAILED")
                        .put("message", "Stream failed")
                        .put("request_id", readRequestID())
                        .encode()));
    }
}
