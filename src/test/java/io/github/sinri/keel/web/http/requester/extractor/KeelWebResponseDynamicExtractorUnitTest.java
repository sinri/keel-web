package io.github.sinri.keel.web.http.requester.extractor;

import io.github.sinri.keel.web.http.requester.error.ReceivedAbnormalStatusResponse;
import io.github.sinri.keel.web.http.requester.error.ReceivedUnexpectedFormatResponse;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeelWebResponseDynamicExtractorUnitTest {
    @Test
    void shouldValidateStatusAndWrapTransformationFailures() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpServer server = null;
        WebClient client = WebClient.create(vertx);
        try {
            Router router = Router.router(vertx);
            router.get("/ok").handler(context -> context.response().end("alpha"));
            router.get("/created").handler(context -> context.response().setStatusCode(201).end("created"));
            router.get("/invalid").handler(context -> context.response().end("not-a-number"));
            server = await(vertx.createHttpServer().requestHandler(router).listen(0));

            int port = server.actualPort();
            HttpResponse<Buffer> ok = await(client.get(port, "localhost", "/ok").send());
            HttpResponse<Buffer> created = await(client.get(port, "localhost", "/created").send());
            HttpResponse<Buffer> invalid = await(client.get(port, "localhost", "/invalid").send());

            var defaultExtractor = new KeelWebResponseDynamicExtractor<>(Buffer::toString);
            assertEquals("alpha", defaultExtractor.extract(ok));

            var createdExtractor = new KeelWebResponseDynamicExtractor<>(
                    "create-request", Buffer::toString, Set.of(201));
            assertEquals("created", createdExtractor.extract(created));

            var statusAgnosticExtractor = new KeelWebResponseDynamicExtractor<String>(
                    "status-agnostic", Buffer::toString, null);
            assertEquals("created", statusAgnosticExtractor.extract(created));

            ReceivedAbnormalStatusResponse abnormal = assertThrows(
                    ReceivedAbnormalStatusResponse.class,
                    () -> defaultExtractor.extract("fetch-request", created));
            assertEquals(201, abnormal.getResponseStatusCode());
            assertEquals("created", abnormal.getResponseBody().toString());
            assertEquals("{fetch-request} received response with abnormal status code (non 200)",
                    abnormal.getMessage());

            var integerExtractor = new KeelWebResponseDynamicExtractor<Integer>(
                    "number-request", body -> Integer.parseInt(body.toString()));
            ReceivedUnexpectedFormatResponse unexpectedFormat = assertThrows(
                    ReceivedUnexpectedFormatResponse.class,
                    () -> integerExtractor.extract(invalid));
            assertEquals(200, unexpectedFormat.getResponseStatusCode());
            assertEquals("not-a-number", unexpectedFormat.getResponseBody().toString());
            assertEquals("{number-request} received response with body in unexpected format",
                    unexpectedFormat.getMessage());
        } finally {
            client.close();
            if (server != null) {
                await(server.close());
            }
            await(vertx.close());
        }
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get();
    }
}
