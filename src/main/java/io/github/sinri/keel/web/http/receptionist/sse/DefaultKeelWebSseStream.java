package io.github.sinri.keel.web.http.receptionist.sse;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One caller-controlled write at a time; no buffering or background producer.
 * @since 5.0.4
 */
@Deprecated
@NullMarked
final class DefaultKeelWebSseStream implements KeelWebSseStream {
    private final HttpServerResponse response;
    private final Promise<Void> completion = Promise.promise();
    private final AtomicBoolean ending = new AtomicBoolean();

    DefaultKeelWebSseStream(HttpServerResponse response) {
        this.response = response;
        response.closeHandler(v -> failAndReset(new IllegalStateException("SSE client disconnected")));
        response.exceptionHandler(this::failAndReset);
    }

    @Override
    public Future<Void> sendEvent(ServerSentEvent event) {
        return write(event.encode());
    }

    @Override
    public Future<Void> sendComment(String comment) {
        return sendEvent(new ServerSentEvent().comment(comment));
    }

    private Future<Void> write(String encoded) {
        if (ending.get() || completion.future().isComplete()) return Future.failedFuture("SSE stream is closed");
        try {
            return response.write(encoded).onFailure(this::failAndReset);
        } catch (RuntimeException error) {
            failAndReset(error);
            return Future.failedFuture(error);
        }
    }

    @Override
    public Future<Void> end() {
        if (ending.get() || completion.future().isComplete()) return completion.future();
        if (ending.compareAndSet(false, true)) {
            try {
                response.end().onComplete(result -> {
                    if (result.failed()) failAndReset(result.cause());
                    else completion.tryComplete();
                });
            } catch (RuntimeException error) {
                failAndReset(error);
            }
        }
        return completion.future();
    }

    @Override
    public Future<Void> completion() {
        return completion.future();
    }

    private void failAndReset(Throwable error) {
        if (!completion.tryFail(error)) return;
        try {
            if (!response.closed()) {
                response.reset().onFailure(resetError -> {
                    if (resetError != error) error.addSuppressed(resetError);
                });
            }
        } catch (RuntimeException resetError) {
            if (resetError != error) error.addSuppressed(resetError);
        }
    }
}
