package io.github.sinri.keel.web.http.receptionist;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.multipart.MultipartForm;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AbstractRequestBodyUnitTest {
    @Test
    void shouldExtractSupportedRequestBodiesAfterBodyHandler() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpServer server = null;
        WebClient client = WebClient.create(vertx, new WebClientOptions().setKeepAlive(false));
        Path uploadSource = Files.createTempFile("keel-web-request-body-", ".txt");
        Path uploadDirectory = Files.createTempDirectory("keel-web-file-uploads-");
        Files.writeString(uploadSource, "uploaded content");
        try {
            Router router = Router.router(vertx);
            router.post().handler(BodyHandler.create(uploadDirectory.toString()));
            router.post().handler(context -> {
                TestRequestBody requestBody = new TestRequestBody(context);
                context.json(new JsonObject()
                        .put("body", requestBody.cloneAsJsonObject())
                        .put("fileUploadCount", context.fileUploads().size()));
            });
            server = await(vertx.createHttpServer().requestHandler(router).listen(0));

            int port = server.actualPort();
            HttpResponse<Buffer> json = await(client.post(port, "localhost", "/")
                    .putHeader("Content-Type", "application/json")
                    .sendBuffer(new JsonObject().put("name", "json").toBuffer()));
            HttpResponse<Buffer> parameterizedJson = await(client.post(port, "localhost", "/")
                    .putHeader("Content-Type", "Application/JSON; Charset=UTF-8")
                    .sendBuffer(new JsonObject().put("name", "parameterized-json").toBuffer()));
            HttpResponse<Buffer> urlEncoded = await(client.post(port, "localhost", "/")
                    .putHeader("Content-Type", "application/x-www-form-urlencoded")
                    .sendBuffer(Buffer.buffer("name=urlencoded&count=2")));
            MultipartForm multipartForm = MultipartForm.create()
                    .attribute("name", "multipart")
                    .binaryFileUpload("attachment", "example.txt", uploadSource.toString(), "text/plain");
            HttpResponse<Buffer> multipart = await(client.post(port, "localhost", "/")
                    .sendMultipartForm(multipartForm));

            assertResponse(json, new JsonObject().put("name", "json"), 0);
            assertResponse(parameterizedJson, new JsonObject().put("name", "parameterized-json"), 0);
            assertResponse(urlEncoded, new JsonObject().put("name", "urlencoded").put("count", "2"), 0);
            assertResponse(multipart, new JsonObject().put("name", "multipart"), 1);
            assertFalse(multipart.bodyAsJsonObject().getJsonObject("body").containsKey("attachment"));
        } finally {
            Files.deleteIfExists(uploadSource);
            try (var paths = Files.walk(uploadDirectory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to clean up test upload path: " + path, e);
                    }
                });
            }
            client.close();
            if (server != null) {
                await(server.close());
            }
            await(vertx.close());
        }
    }

    private static void assertResponse(HttpResponse<Buffer> response, JsonObject expectedBody, int fileUploadCount) {
        assertEquals(200, response.statusCode());
        JsonObject responseBody = response.bodyAsJsonObject();
        assertEquals(expectedBody, responseBody.getJsonObject("body"));
        assertEquals(fileUploadCount, responseBody.getInteger("fileUploadCount"));
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get();
    }

    private static final class TestRequestBody extends AbstractRequestBody {
        private TestRequestBody(io.vertx.ext.web.RoutingContext routingContext) {
            super(routingContext);
        }
    }
}
