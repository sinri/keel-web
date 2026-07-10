package io.github.sinri.keel.web.http.fastdocs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeelFastDocsKitUnitTest {
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
}
