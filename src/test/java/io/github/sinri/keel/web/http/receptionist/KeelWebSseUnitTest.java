package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.web.http.receptionist.sse.ServerSentEvent;
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
    void streamsEventsBeforeExplicitEndAndAllowsDisconnectCleanup() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpClient client = vertx.createHttpClient();
        Promise<Void> release = Promise.promise();
        Promise<Void> serverEnded = Promise.promise();
        Promise<Void> disconnected = Promise.promise();
        var router = io.vertx.ext.web.Router.router(vertx);
        router.route().handler(ctx -> {
            ctx.put(io.github.sinri.keel.web.http.prehandler.KeelPlatformHandler.KEEL_REQUEST_ID, "test");
            new KeelWebSseReceptionist(ctx) {
                @Override
                public void handle() {
                    setHeadersForSSE(ctx);
                    ctx.response().closeHandler(v -> disconnected.tryComplete());
                    pushOneEvent(new ServerSentEvent().comment("heartbeat"))
                            .compose(v -> pushOneEvent(new ServerSentEvent().data("first")))
                            .compose(v -> ctx.request().path().equals("/disconnect") ? Promise.<Void>promise().future() : release.future())
                            .compose(v -> ctx.response().end())
                            .onComplete(serverEnded);
                }
            }.handle();
        });
        HttpServer server = await(vertx.createHttpServer().requestHandler(router).listen(0));
        try {
            var response = await(await(client.request(HttpMethod.GET, server.actualPort(), "localhost", "/"))
                    .send().map(r -> { r.pause(); return r; }));
            assertEquals("text/event-stream; charset=utf-8", response.getHeader("Content-Type"));
            assertEquals("no-cache, no-transform", response.getHeader("Cache-Control"));
            assertEquals("no", response.getHeader("X-Accel-Buffering"));
            Promise<Void> received = Promise.promise();
            Promise<Void> ended = Promise.promise();
            StringBuilder body = new StringBuilder();
            response.handler(buffer -> {
                body.append(buffer.toString());
                if (body.toString().contains("data: first\n\n")) received.tryComplete();
            });
            response.endHandler(v -> ended.tryComplete());
            response.resume();
            await(received.future());
            assertFalse(serverEnded.future().isComplete());
            release.complete();
            await(serverEnded.future());
            await(ended.future());
            assertEquals(": heartbeat\n\ndata: first\n\n", body.toString());

            // A second, unfinished response lets the subclass observe client disconnect.
            var second = await(await(client.request(HttpMethod.GET, server.actualPort(), "localhost", "/disconnect"))
                    .send());
            await(client.close());
            await(disconnected.future());
        } finally {
            await(client.close());
            await(server.close());
            await(vertx.close());
        }
    }

    @Test
    void encodingAndWriteErrorsAreFailedFutures() throws Exception {
        Vertx vertx = Vertx.vertx();
        var client = io.vertx.ext.web.client.WebClient.create(vertx);
        var router = io.vertx.ext.web.Router.router(vertx);
        Promise<Void> checked = Promise.promise();
        router.route().handler(ctx -> {
            try {
                ctx.put(io.github.sinri.keel.web.http.prehandler.KeelPlatformHandler.KEEL_REQUEST_ID, "test");
                Promise<Void> write = Promise.promise();
                var failure = new IllegalStateException("write failed");
                AtomicReference<Boolean> throwOnWrite = new AtomicReference<>(true);
                HttpServerResponse fakeResponse = (HttpServerResponse) Proxy.newProxyInstance(
                        HttpServerResponse.class.getClassLoader(), new Class<?>[]{HttpServerResponse.class},
                        (proxy, method, args) -> {
                            if (!method.getName().equals("write")) throw new AssertionError("Unexpected " + method);
                            if (throwOnWrite.get()) throw failure;
                            return write.future();
                        });
                var fakeContext = (io.vertx.ext.web.RoutingContext) Proxy.newProxyInstance(
                        io.vertx.ext.web.RoutingContext.class.getClassLoader(),
                        new Class<?>[]{io.vertx.ext.web.RoutingContext.class},
                        (proxy, method, args) -> method.getName().equals("response")
                                ? fakeResponse : method.invoke(ctx, args));
                var receptionist = new KeelWebSseReceptionist(fakeContext) {
                    @Override
                    public void handle() { }
                };
                Future<Void> invalid = assertDoesNotThrow(() -> receptionist.pushOneEvent(new ServerSentEvent()));
                assertInstanceOf(NullPointerException.class, invalid.cause());
                Future<Void> sync = assertDoesNotThrow(() -> receptionist.pushOneEvent(new ServerSentEvent().data("test")));
                assertSame(failure, sync.cause());
                throwOnWrite.set(false);
                Future<Void> async = receptionist.pushOneEvent(new ServerSentEvent().data("test"));
                assertFalse(async.isComplete());
                write.fail(failure);
                assertSame(failure, async.cause());
                checked.complete();
                ctx.response().end("ok");
            } catch (Throwable error) {
                checked.tryFail(error);
                ctx.response().setStatusCode(500).end();
            }
        });
        HttpServer server = await(vertx.createHttpServer().requestHandler(router).listen(0));
        try {
            await(client.get(server.actualPort(), "localhost", "/").send());
            await(checked.future());
        } finally {
            client.close();
            await(server.close());
            await(vertx.close());
        }
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
}
