package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.web.http.receptionist.sse.ServerSentEvent;
import io.github.sinri.keel.web.http.receptionist.sse.KeelWebSseStream;
import io.vertx.core.*;
import io.vertx.core.http.*;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

@NullMarked
class KeelWebSseUnitTest {
    @Test
    void encodesMultilineAndRejectsFieldInjection() {
        assertEquals("id: \nevent: delta\nretry: 0\ndata: a\ndata: b\ndata: \n\n",
                new ServerSentEvent().data("a\r\nb\r").event("delta").id("").retry(0L).encode());
        assertEquals("data: \n\n", new ServerSentEvent().data("").encode());
        assertThrows(IllegalArgumentException.class, () -> new ServerSentEvent().data("").event("a\nb"));
        assertThrows(IllegalArgumentException.class, () -> new ServerSentEvent().data("").id("\0"));
        assertThrows(IllegalArgumentException.class, () -> new ServerSentEvent().data("").retry(-1L));
    }

    @Test
    void encodesCommentsAloneAndAlongsideData() {
        assertEquals(": heartbeat\n\n", new ServerSentEvent().comment("heartbeat").encode());
        assertEquals(": a\n: b\n: \n\n", new ServerSentEvent().comment("a\r\nb\r").encode());
        assertEquals(": \n\n", new ServerSentEvent().comment("").encode());
        assertEquals(": note\nevent: delta\ndata: value\n\n",
                new ServerSentEvent().comment("note").event("delta").data("value").encode());
        assertThrows(NullPointerException.class, () -> new ServerSentEvent().comment(null));
    }

    @Test
    void fluentEventRequiresDataAndCanBeUpdated() {
        assertThrows(NullPointerException.class, () -> new ServerSentEvent().encode());
        var event = new ServerSentEvent();
        assertSame(event, event.data("first"));
        String first = event.encode();
        assertSame(event, event.data("second").event("delta").id("42").retry(1000L));
        assertEquals("data: first\n\n", first);
        assertEquals("id: 42\nevent: delta\nretry: 1000\ndata: second\n\n", event.encode());
        assertThrows(NullPointerException.class, () -> event.event(null));
        assertThrows(NullPointerException.class, () -> event.id(null));
    }

    @Test
    void deliversBeforeEndAndEndsIdempotently() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpClient client = vertx.createHttpClient();
        AtomicReference<KeelWebSseStream> streamRef = new AtomicReference<>();
        HttpServer server = await(vertx.createHttpServer().requestHandler(request -> {
            request.response().setChunked(true).putHeader("Content-Type", "text/event-stream");
            KeelWebSseStream stream = KeelWebSseStream.createDefaultInstance(request.response());
            streamRef.set(stream);
            stream.sendComment("connected").compose(v -> stream.sendEvent(new ServerSentEvent().data("first")));
        }).listen(0));
        try {
            HttpClientResponse response = await(await(client.request(HttpMethod.GET, server.actualPort(), "localhost", "/")).send().map(r -> { r.pause(); return r; }));
            Promise<Void> first = Promise.promise();
            Promise<Void> ended = Promise.promise();
            StringBuilder body = new StringBuilder();
            response.handler(buffer -> {
                body.append(buffer.toString());
                if (body.toString().contains("data: first\n\n")) first.tryComplete();
            });
            response.endHandler(v -> ended.tryComplete());
            response.resume();
            await(first.future());
            assertFalse(streamRef.get().completion().isComplete());
            Future<Void> end = streamRef.get().end();
            assertSame(end, streamRef.get().end());
            await(end);
            await(ended.future());
            assertTrue(streamRef.get().sendEvent(new ServerSentEvent().data("late")).failed());
            assertEquals(": connected\n\ndata: first\n\n", body.toString());
        } finally {
            await(client.close());
            await(server.close());
            await(vertx.close());
        }
    }

    @Test
    void returnsResponseWriteFutureForChaining() throws Exception {
        FakeResponse fake = new FakeResponse();
        KeelWebSseStream stream = KeelWebSseStream.createDefaultInstance(fake.response);
        Future<Void> sent = stream.sendEvent(new ServerSentEvent().data("hello"));
        assertSame(fake.write.future(), sent);
        Future<Void> ended = sent.compose(v -> stream.end());
        assertFalse(ended.isComplete());
        fake.write.complete();
        await(ended);
        assertTrue(stream.completion().succeeded());
    }

    @Test
    void writeFailureResetsResponseAndPreventsFurtherWrites() {
        FakeResponse fake = new FakeResponse();
        KeelWebSseStream stream = KeelWebSseStream.createDefaultInstance(fake.response);
        Future<Void> sent = stream.sendComment("hello");
        var error = new IllegalStateException("write failed");
        fake.write.fail(error);
        assertSame(error, sent.cause());
        assertSame(error, stream.completion().cause());
        assertTrue(fake.reset);
        assertTrue(stream.sendComment("late").failed());
        assertSame(stream.completion(), stream.end());
    }

    @Test
    void endFailureResetsOnceAndPreservesOriginalError() {
        FakeResponse fake = new FakeResponse();
        var original = new IllegalStateException("end failed");
        var resetError = new IllegalStateException("reset failed");
        fake.endResult = Future.failedFuture(original);
        fake.resetResult = Future.failedFuture(resetError);
        var stream = KeelWebSseStream.createDefaultInstance(fake.response);
        assertSame(original, stream.end().cause());
        assertSame(original, stream.completion().cause());
        assertTrue(fake.reset);
        assertArrayEquals(new Throwable[]{resetError}, original.getSuppressed());
        fake.close.handle(null);
        assertEquals(1, fake.resetCount);
        assertSame(stream.completion(), stream.end());
    }

    @Test
    void synchronousWriteFailureSafelyAborts() {
        FakeResponse fake = new FakeResponse();
        fake.writeError = new IllegalStateException("write threw");
        KeelWebSseStream stream = KeelWebSseStream.createDefaultInstance(fake.response);
        assertSame(fake.writeError, stream.sendComment("hello").cause());
        assertTrue(fake.reset);
        assertTrue(stream.completion().failed());
    }

    @Test
    void disconnectFailsPendingWrites() throws Exception {
        Vertx vertx = Vertx.vertx();
        try {
            FakeResponse fake = new FakeResponse();
            KeelWebSseStream stream = KeelWebSseStream.createDefaultInstance(fake.response);
            Future<Void> sent = stream.sendEvent(new ServerSentEvent().data("hello"));
            fake.close.handle(null);
            assertTrue(sent.failed());
            assertTrue(stream.completion().failed());
            assertTrue(stream.end().failed());
        } finally { await(vertx.close()); }
    }

    @Test
    void receptionistHandlesSuccessAndErrorsBeforeAndAfterOpening() throws Exception {
        Vertx vertx = Vertx.vertx();
        io.vertx.ext.web.Router router = io.vertx.ext.web.Router.router(vertx);
        router.route().handler(ctx -> {
            ctx.put(io.github.sinri.keel.web.http.prehandler.KeelPlatformHandler.KEEL_REQUEST_ID, "test-request");
            new KeelWebSseReceptionist(ctx) {
                @Override
                protected Future<Void> prepare() {
                    if (ctx.request().path().equals("/before")) return Future.failedFuture("private detail");
                    if (ctx.request().path().equals("/throw-before")) throw new IllegalStateException("private detail");
                    return Future.succeededFuture();
                }

                @Override
                protected Future<Void> handleStream(KeelWebSseStream stream) {
                    if (ctx.request().path().equals("/after")) throw new IllegalStateException("private detail");
                    return stream.sendEvent(new ServerSentEvent().data("one")).compose(v -> stream.sendEvent(new ServerSentEvent().data("two")));
                }
            }.handle();
        });
        router.route().failureHandler(ctx -> ctx.response().setStatusCode(500).end("rejected"));
        HttpServer server = await(vertx.createHttpServer().requestHandler(router).listen(0));
        io.vertx.ext.web.client.WebClient client = io.vertx.ext.web.client.WebClient.create(vertx);
        try {
            var success = await(client.get(server.actualPort(), "localhost", "/ok").send());
            assertEquals(200, success.statusCode());
            assertEquals("text/event-stream; charset=utf-8", success.getHeader("Content-Type"));
            assertEquals("data: one\n\ndata: two\n\n", success.bodyAsString());
            var before = await(client.get(server.actualPort(), "localhost", "/before").send());
            assertEquals(500, before.statusCode());
            assertEquals("rejected", before.bodyAsString());
            for (String path : new String[]{"/throw-before"}) {
                var rejected = await(client.get(server.actualPort(), "localhost", path).send());
                assertEquals(500, rejected.statusCode());
                assertEquals("rejected", rejected.bodyAsString());
            }
            var after = await(client.get(server.actualPort(), "localhost", "/after").send());
            assertEquals(200, after.statusCode());
            assertTrue(after.bodyAsString().contains("event: error\n"));
            assertFalse(after.bodyAsString().contains("private detail"));
        } finally {
            client.close();
            await(server.close());
            await(vertx.close());
        }
    }

    @Test
    void explicitHeartbeatAndRealDisconnectNotifyProducer() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpClient client = vertx.createHttpClient();
        Promise<Void> disconnected = Promise.promise();
        HttpServer server = await(vertx.createHttpServer().requestHandler(request -> {
            request.response().setChunked(true).putHeader("Content-Type", "text/event-stream");
            var stream = KeelWebSseStream.createDefaultInstance(request.response());
            stream.completion().onFailure(error -> disconnected.tryComplete());
            stream.sendComment("connected").compose(v -> stream.sendComment("heartbeat"));
        }).listen(0));
        try {
            var response = await(await(client.request(HttpMethod.GET, server.actualPort(), "localhost", "/"))
                    .send().map(r -> { r.pause(); return r; }));
            Promise<Void> heartbeat = Promise.promise();
            StringBuilder received = new StringBuilder();
            response.handler(buffer -> {
                received.append(buffer.toString());
                if (received.toString().contains(": heartbeat\n\n")) heartbeat.tryComplete();
            });
            response.resume();
            await(heartbeat.future());
            await(client.close());
            await(disconnected.future());
        } finally {
            await(client.close());
            await(server.close());
            await(vertx.close());
        }
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private static final class FakeResponse {
        volatile boolean reset;
        int resetCount;
        Future<Void> endResult = Future.succeededFuture();
        Future<Void> resetResult = Future.succeededFuture();
        RuntimeException writeError;
        Handler<Void> close;
        Promise<Void> write = Promise.promise();
        @SuppressWarnings("unchecked")
        HttpServerResponse response = (HttpServerResponse) Proxy.newProxyInstance(
                HttpServerResponse.class.getClassLoader(), new Class<?>[]{HttpServerResponse.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "write" -> {
                            if (writeError != null) throw writeError;
                            yield write.future();
                        }
                        case "closed" -> false;
                        case "closeHandler" -> { close = (Handler<Void>) args[0]; yield proxy; }
                        case "exceptionHandler" -> proxy;
                        case "end" -> endResult;
                        case "reset" -> {
                            reset = true;
                            resetCount++;
                            write.tryFail("response reset");
                            yield resetResult;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
    }
}
