package io.github.sinri.keel.web.http.fastdocs;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeelFastDocsKitUnitTest {
    @Test
    void constructorShouldRejectAnEmptyMarkdownRoot() {
        assertThrows(IllegalArgumentException.class, () -> new KeelFastDocsKit("/docs/", ""));
        assertThrows(IllegalArgumentException.class, () -> new KeelFastDocsKit("/docs/", "///"));
    }

    @Test
    void installToRouterShouldNormalizePathsAndInstallIndependentSites() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpServer server = null;
        WebClient client = WebClient.create(vertx);
        try {
            Router router = Router.router(vertx);
            KeelFastDocsKit.installToRouter(
                    router, "/alpha", "fastdocs-test/alpha", "Alpha Docs", "Alpha Footer");
            KeelFastDocsKit.installToRouter(
                    router, "/beta", "fastdocs-test/beta", "Beta Docs", "Beta Footer");
            server = await(vertx.createHttpServer().requestHandler(router).listen(0));

            HttpResponse<Buffer> alpha = await(client.get(server.actualPort(), "localhost", "/alpha/catalogue").send());
            HttpResponse<Buffer> beta = await(client.get(server.actualPort(), "localhost", "/beta/catalogue").send());
            HttpResponse<Buffer> alphaIndex = await(client.get(
                    server.actualPort(), "localhost", "/alpha/index.md").send());
            HttpResponse<Buffer> alphaAsset = await(client.get(
                    server.actualPort(), "localhost", "/alpha/assets/example.svg").send());
            HttpResponse<Buffer> assetFromWrongSite = await(client.get(
                    server.actualPort(), "localhost", "/beta/assets/example.svg").send());
            HttpResponse<Buffer> missingAsset = await(client.get(
                    server.actualPort(), "localhost", "/alpha/assets/missing.svg").send());
            HttpResponse<Buffer> traversal = await(client.get(
                    server.actualPort(), "localhost", "/alpha/../beta/index.md").send());
            HttpResponse<Buffer> encodedTraversal = await(client.get(
                    server.actualPort(), "localhost", "/alpha/%2e%2e/beta/index.md").send());
            HttpResponse<Buffer> nonGet = await(client.request(
                    HttpMethod.POST, server.actualPort(), "localhost", "/alpha/catalogue").send());

            assertEquals(200, alpha.statusCode());
            assertTrue(alpha.bodyAsString().contains("Alpha Docs"));
            assertTrue(alpha.bodyAsString().contains("/alpha/alpha-page.md"));
            assertFalse(alpha.bodyAsString().contains("/beta/beta-page.md"));
            assertEquals(200, beta.statusCode());
            assertTrue(beta.bodyAsString().contains("Beta Docs"));
            assertTrue(beta.bodyAsString().contains("/beta/beta-page.md"));
            assertFalse(beta.bodyAsString().contains("/alpha/alpha-page.md"));
            assertEquals(200, alphaIndex.statusCode());
            assertTrue(alphaIndex.bodyAsString().contains("src=\"assets/example.svg\""));
            assertEquals(200, alphaAsset.statusCode());
            assertEquals("image/svg+xml", alphaAsset.getHeader("Content-Type"));
            assertTrue(alphaAsset.bodyAsString().contains("fastdocs-test-asset"));
            assertEquals(404, assetFromWrongSite.statusCode());
            assertEquals(404, missingAsset.statusCode());
            assertEquals(404, traversal.statusCode());
            assertEquals(404, encodedTraversal.statusCode());
            assertEquals(405, nonGet.statusCode());
        } finally {
            client.close();
            if (server != null) {
                await(server.close());
            }
            await(vertx.close());
        }
    }

    @Test
    void catalogueCacheShouldBeIsolatedByKitInstance() {
        KeelFastDocsKit alphaKit = new KeelFastDocsKit("/alpha/", "fastdocs-test/alpha/");
        KeelFastDocsKit betaKit = new KeelFastDocsKit("/beta/", "fastdocs-test/beta/");

        String alphaCatalogue = alphaKit.getCatalogueDivContent(options(
                "/alpha/", "fastdocs-test/alpha/", "Alpha Docs", null));
        String betaCatalogue = betaKit.getCatalogueDivContent(options(
                "/beta/", "fastdocs-test/beta/", "Beta Docs", null));

        assertTrue(alphaCatalogue.contains("Alpha Docs"));
        assertTrue(alphaCatalogue.contains("/alpha/alpha-page.md"));
        assertFalse(alphaCatalogue.contains("/beta/beta-page.md"));
        assertTrue(betaCatalogue.contains("Beta Docs"));
        assertTrue(betaCatalogue.contains("/beta/beta-page.md"));
        assertFalse(betaCatalogue.contains("/alpha/alpha-page.md"));
    }

    @Test
    void catalogueCacheShouldKeepSeparateStandaloneAndInDocumentViews() {
        KeelFastDocsKit kit = new KeelFastDocsKit("/alpha/", "fastdocs-test/alpha/");

        String standalone = kit.getCatalogueDivContent(options(
                "/alpha/", "fastdocs-test/alpha/", "Alpha Docs", null));
        String inDocument = kit.getCatalogueDivContent(options(
                "/alpha/", "fastdocs-test/alpha/", "Alpha Docs", "index.md"));

        assertFalse(standalone.contains("target='_parent'"));
        assertTrue(inDocument.contains("target='_parent'"));
    }

    @Test
    void catalogueCacheShouldReuseTheFirstGeneratedValue() {
        KeelFastDocsKit kit = new KeelFastDocsKit("/alpha/", "fastdocs-test/alpha/");
        PageBuilderOptions options = options(
                "/alpha/", "fastdocs-test/alpha/", "Alpha Docs", null);

        String first = kit.getCatalogueDivContent(options);
        String second = kit.getCatalogueDivContent(options);

        assertSame(first, second);
    }

    @Test
    void catalogueCacheShouldInitializeSafelyUnderConcurrentAccess() throws Exception {
        KeelFastDocsKit kit = new KeelFastDocsKit("/alpha/", "fastdocs-test/alpha/");
        PageBuilderOptions options = options(
                "/alpha/", "fastdocs-test/alpha/", "Alpha Docs", null);
        List<Callable<String>> requests = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            requests.add(() -> kit.getCatalogueDivContent(options));
        }

        var executor = Executors.newFixedThreadPool(8);
        try {
            var results = executor.invokeAll(requests);
            String cached = results.get(0).get();
            for (var result : results) {
                assertSame(cached, result.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void specialPathsAndGeneratedLinksShouldResolveToTheCorrectDocument() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpServer server = null;
        WebClient client = WebClient.create(vertx);
        try {
            Router router = Router.router(vertx);
            KeelFastDocsKit.installToRouter(router, "/docs/", "fastdocs-test/paths", "Path Docs", "Footer");
            server = await(vertx.createHttpServer().requestHandler(router).listen(0));
            int port = server.actualPort();
            var catalogue = await(client.get(port, "localhost", "/docs/catalogue").send());
            assertEquals(200, catalogue.statusCode());
            String[][] cases = {
                    {"C%2B%2B.md", "C++.md"},
                    {"C%20%20.md", "C  .md"},
                    {"%E4%B8%AD%E6%96%87.md", "中文.md"},
                    {"100%25.md", "100%.md"},
                    {"a%23b%3F.md", "a#b?.md"},
                    {"%252e%252e.md", "%2e%2e.md"},
                    {"sub%20%2B%E4%B8%AD%E6%96%87/C%2B%2B.md", "sub +中文/C++.md"}
            };
            for (String[] entry : cases) {
                String href = "/docs/" + entry[0];
                // Follow the exact href emitted by the catalogue, including its percent escapes.
                assertTrue(catalogue.bodyAsString().contains("href='" + href + "'"), href);
                var response = await(client.get(port, "localhost", href).send());
                assertEquals(200, response.statusCode(), href);
                assertTrue(response.bodyAsString().contains("file-marker: " + entry[1]), href);
                assertTrue(response.bodyAsString().contains("<title>Path Docs - " + entry[1] + "</title>"), href);
                assertTrue(response.bodyAsString().contains("href='" + href + "'"), href);
            }
            var literalPlus = await(client.get(port, "localhost", "/docs/C++.md").send());
            assertEquals(200, literalPlus.statusCode());
            assertTrue(literalPlus.bodyAsString().contains("file-marker: C++.md"));
            assertFalse(literalPlus.bodyAsString().contains("file-marker: C  .md"));
            assertTrue(literalPlus.bodyAsString().contains("?from_doc=C%2B%2B.md"));
            var nested = await(client.get(port, "localhost", "/docs/" + cases[6][0]).send());
            assertTrue(nested.bodyAsString().contains("href='/docs/sub%20%2B%E4%B8%AD%E6%96%87/index.md'"));
            for (String path : List.of("%2e%2e/alpha/index.md", "%2e%2e%2falpha/index.md", "%2Fetc/index.md", "bad%GG.md")) {
                var response = await(client.get(port, "localhost", "/docs/" + path).send());
                assertTrue(response.statusCode() >= 400 && response.statusCode() < 500, path);
                String body = java.util.Objects.requireNonNullElse(response.bodyAsString(), "");
                assertFalse(body.contains("assets/example.svg"), path);
                assertFalse(body.contains("file-marker:"), path);
            }
        } finally {
            client.close();
            if (server != null) await(server.close());
            await(vertx.close());
        }
    }

    private static PageBuilderOptions options(
            String rootURLPath,
            String rootMarkdownFilePath,
            String subject,
            String fromDoc
    ) {
        PageBuilderOptions options = new PageBuilderOptions();
        options.rootURLPath = rootURLPath;
        options.rootMarkdownFilePath = rootMarkdownFilePath;
        options.subjectOfDocuments = subject;
        options.fromDoc = fromDoc;
        return options;
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get();
    }
}
