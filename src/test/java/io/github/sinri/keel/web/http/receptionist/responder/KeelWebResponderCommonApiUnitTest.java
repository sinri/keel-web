package io.github.sinri.keel.web.http.receptionist.responder;

import io.github.sinri.keel.core.utils.value.ValueBox;
import io.github.sinri.keel.logger.api.LogLevel;
import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.github.sinri.keel.logger.api.logger.BaseSpecificLogger;
import io.github.sinri.keel.web.http.prehandler.KeelPlatformHandler;
import io.github.sinri.keel.web.http.receptionist.ReceptionistSpecificLog;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class KeelWebResponderCommonApiUnitTest {
    private static final String SECRET = "probe-secret-marker";
    private static final String REQUEST_ID = "request-issue-7";

    @Test
    void internalErrorsAreSafeRegardlessOfDebugAndRemainInServerLogs() throws Exception {
        var cause = new IllegalStateException(SECRET);
        var error = KeelWebApiError.wrap(cause);
        assertSame(cause, error.getCause());
        for (LogLevel level : List.of(LogLevel.INFO, LogLevel.DEBUG)) {
            var result = request(level, responder -> responder.respondOnFailure(error));
            assertFailure(result.response(), 500, error.getClass(), "Internal server error");
            assertNull(result.response().bodyAsJsonObject().getValue("data"));
            var log = result.logs().stream().filter(item -> item.level() == LogLevel.ERROR).findFirst().orElseThrow();
            assertSame(error, log.exception());
            assertEquals(SECRET, log.exception().getCause().getMessage());
            assertTrue(log.exception().getCause().getStackTrace().length > 0);
            assertEquals(REQUEST_ID, log.extra().get("request_id"));
        }
    }

    @Test
    void businessErrorsKeepPublicMessageTypeStatusAndExtraButHideCause() throws Exception {
        var error = new PublicBusinessError(new IllegalArgumentException(SECRET));
        assertSame(error, KeelWebApiError.wrap(error));
        var extra = new JsonObject().put("field", "email");
        var result = request(LogLevel.INFO,
                responder -> responder.respondOnFailure(error, new ValueBox<>(extra)));
        assertFailure(result.response(), 422, PublicBusinessError.class, "Invalid parameters");
        assertEquals(extra, result.response().bodyAsJsonObject().getJsonObject("data").getJsonObject("extra"));
    }

    @Test
    void defaultBusinessStatusRemains200() throws Exception {
        var result = request(LogLevel.INFO,
                responder -> responder.respondOnFailure(new KeelWebApiError("User not found")));
        assertFailure(result.response(), 200, KeelWebApiError.class, "User not found");
    }

    @Test
    void missingAndNullExtraValuesUseSafeFallback() throws Exception {
        for (ValueBox<?> box : List.of(new ValueBox<>(), new ValueBox<>(null))) {
            var result = request(LogLevel.INFO,
                    responder -> responder.respondOnFailure(KeelWebApiError.wrap(new RuntimeException(SECRET)), box));
            assertFailure(result.response(), 500, KeelWebApiError.class, "Internal server error");
            assertEquals("Unable to render extra data",
                    result.response().bodyAsJsonObject().getJsonObject("data").getString("extra_render_error"));
            assertEquals(2, result.logs().size());
        }
    }

    @Test
    void extraSerializationFailureUsesSafeFallbackAndIsLogged() throws Exception {
        var result = request(LogLevel.INFO, responder -> responder.respondOnFailure(
                new KeelWebApiError("Invalid parameters"), new ValueBox<>(new BrokenExtra())));
        assertFailure(result.response(), 200, KeelWebApiError.class, "Invalid parameters");
        assertEquals("Unable to render extra data",
                result.response().bodyAsJsonObject().getJsonObject("data").getString("extra_render_error"));
        assertEquals(2, result.logs().size());
        assertNotNull(result.logs().get(1).exception());
        assertEquals(REQUEST_ID, result.logs().get(1).extra().get("request_id"));
    }

    @Test
    void successResponseIsUnchanged() throws Exception {
        var result = request(LogLevel.INFO,
                responder -> responder.respondOnSuccess(new JsonObject().put("id", 123)));
        assertEquals(200, result.response().statusCode());
        assertEquals(new JsonObject().put("request_id", REQUEST_ID).put("code", "OK")
                .put("data", new JsonObject().put("id", 123)), result.response().bodyAsJsonObject());
        assertTrue(result.logs().isEmpty());
    }

    private static void assertFailure(HttpResponse<Buffer> response, int status, Class<?> type, String message) {
        assertEquals(status, response.statusCode());
        assertTrue(response.getHeader("Content-Type").startsWith("application/json"));
        var body = response.bodyAsJsonObject();
        assertEquals(Set.of("request_id", "code", "data", "throwable"), body.fieldNames());
        assertEquals(REQUEST_ID, body.getString("request_id"));
        assertEquals("FAILED", body.getString("code"));
        assertEquals(new JsonObject().put("class", type.getName()).put("message", message), body.getJsonObject("throwable"));
        assertFalse(response.bodyAsString().contains(SECRET));
    }

    private static Result request(LogLevel level, Consumer<KeelWebResponder<JsonObject>> handler) throws Exception {
        Vertx vertx = Vertx.vertx();
        WebClient client = WebClient.create(vertx);
        HttpServer server = null;
        List<SpecificLog<?>> logs = new CopyOnWriteArrayList<>();
        var logger = new BaseSpecificLogger<>("test", () -> new ReceptionistSpecificLog("unset"),
                (topic, log) -> logs.add(log)).visibleLevel(level);
        try {
            Router router = Router.router(vertx);
            router.get("/").handler(context -> {
                context.put(KeelPlatformHandler.KEEL_REQUEST_ID, REQUEST_ID);
                handler.accept(KeelWebResponder.createCommonInstance(context, logger));
            });
            server = await(vertx.createHttpServer().requestHandler(router).listen(0));
            return new Result(await(client.get(server.actualPort(), "localhost", "/").send()), logs);
        } finally {
            client.close();
            if (server != null) await(server.close());
            await(vertx.close());
        }
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private record Result(HttpResponse<Buffer> response, List<SpecificLog<?>> logs) { }

    private static final class PublicBusinessError extends KeelWebApiError {
        private PublicBusinessError(Throwable cause) {
            super(422, "Invalid parameters", cause);
        }
    }

    public static final class BrokenExtra {
        public String getValue() {
            throw new IllegalStateException(SECRET);
        }
    }
}
