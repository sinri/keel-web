package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.logger.api.factory.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeelWebReceptionistLoaderUnitTest {
    @Test
    void shouldRegisterAnnotatedPathAndAllowedMethod() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpServer server = null;
        WebClient client = WebClient.create(vertx);
        TestReceptionist.handledCount.set(0);
        try {
            Router router = Router.router(vertx);
            KeelWebReceptionistLoader.loadClass(
                    router,
                    TestReceptionist.class,
                    LoggerFactory.getShared().createLogger("KeelWebReceptionistLoaderUnitTest"));
            server = await(vertx.createHttpServer().requestHandler(router).listen(0));

            int port = server.actualPort();
            HttpResponse<Buffer> getResponse = await(client.get(port, "localhost", "/loader-test").send());
            HttpResponse<Buffer> postResponse = await(client.request(
                    HttpMethod.POST, port, "localhost", "/loader-test").send());

            assertEquals(200, getResponse.statusCode());
            assertEquals(new JsonObject().put("handled", true), getResponse.bodyAsJsonObject());
            assertEquals(405, postResponse.statusCode());
            assertEquals(1, TestReceptionist.handledCount.get());
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

    @ApiMeta(routePath = "/loader-test", allowMethods = {"GET"}, requestBodyNeeded = false, timeout = 0)
    public static final class TestReceptionist extends KeelWebReceptionist {
        private static final AtomicInteger handledCount = new AtomicInteger();

        public TestReceptionist(RoutingContext routingContext) {
            super(routingContext);
        }

        @Override
        public void handle() {
            handledCount.incrementAndGet();
            getRoutingContext().json(new JsonObject().put("handled", true));
        }
    }
}
