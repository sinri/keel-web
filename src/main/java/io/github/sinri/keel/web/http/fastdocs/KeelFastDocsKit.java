package io.github.sinri.keel.web.http.fastdocs;

import io.github.sinri.keel.web.http.fastdocs.page.CataloguePageBuilder;
import io.github.sinri.keel.web.http.fastdocs.page.FastDocsPathCodec;
import io.github.sinri.keel.web.http.fastdocs.page.MarkdownCssBuilder;
import io.github.sinri.keel.web.http.fastdocs.page.MarkdownPageBuilder;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 提供一个基于 Markdown 文件系统的文档系统。
 *
 * @since 5.0.0
 */
@NullMarked
public class KeelFastDocsKit {
    private final StaticHandler staticHandler;
    private final String rootURLPath;
    private final String rootMarkdownFilePath;
    private final Object catalogueCacheLock = new Object();

    private String documentSubject = "FastDocs";
    private String footerText = "Public Domain";
    private volatile CataloguePageBuilder.@Nullable TreeNode catalogueTreeCache;
    private volatile @Nullable String standaloneCatalogueDivContentCache;
    private volatile @Nullable String inDocumentCatalogueDivContentCache;

    /**
     * Creates a FastDocs site backed by a Markdown resource directory. The Markdown root
     * may be supplied with or without a trailing slash. Internally, the Markdown reader
     * keeps one trailing slash for resource-name concatenation, while the static resource
     * handler uses the same root without trailing slashes.
     *
     * @param rootURLPath such as {@code /prefix/}
     * @param rootMarkdownFilePath such {@code path/to/dir}
     * @throws IllegalArgumentException if {@code rootMarkdownFilePath} is empty
     */
    public KeelFastDocsKit(String rootURLPath, String rootMarkdownFilePath) {
        String normalizedMarkdownRoot = normalizeMarkdownRoot(rootMarkdownFilePath);
        this.staticHandler = StaticHandler.create(normalizeStaticRoot(normalizedMarkdownRoot));
        this.rootURLPath = rootURLPath;
        this.rootMarkdownFilePath = normalizedMarkdownRoot;
    }

    /**
     * Normalizes the root used when Markdown resource names are concatenated.
     *
     * @param rootMarkdownFilePath configured Markdown resource root
     * @return the resource root with exactly one trailing slash
     * @throws IllegalArgumentException if the configured root is empty
     */
    private static String normalizeMarkdownRoot(String rootMarkdownFilePath) {
        String staticRoot = normalizeStaticRoot(rootMarkdownFilePath);
        if (staticRoot.isEmpty()) {
            throw new IllegalArgumentException("rootMarkdownFilePath cannot be empty");
        }
        return staticRoot + "/";
    }

    /**
     * Converts the Markdown resource root to the form expected by Vert.x's static handler.
     * Markdown paths retain their trailing slash for resource-name concatenation, whereas
     * the static handler appends a request path that already starts with a slash.
     *
     * @param rootMarkdownFilePath Markdown resource root configured for this FastDocs site
     * @return the resource root without trailing slashes
     */
    private static String normalizeStaticRoot(String rootMarkdownFilePath) {
        int end = rootMarkdownFilePath.length();
        while (end > 0 && rootMarkdownFilePath.charAt(end - 1) == '/') {
            end--;
        }
        return rootMarkdownFilePath.substring(0, end);
    }

    /**
     * Installs a FastDocs site into a router. The URL base and Markdown resource root may
     * be supplied with or without trailing slashes; both are normalized before requests
     * are handled.
     *
     * @param router router on which the FastDocs wildcard route is installed
     * @param urlPathBase URL base such as {@code /fast-docs/}
     * @param docsDirPathBase classpath resource root such as {@code webroot/markdown/}
     * @param subject document subject displayed in generated pages
     * @param footer footer text displayed in generated pages
     */
    public static void installToRouter(
            Router router,
            String urlPathBase,
            String docsDirPathBase,
            String subject,
            String footer
    ) {
        if (!urlPathBase.endsWith("/")) {
            urlPathBase = urlPathBase + "/";
        }
        KeelFastDocsKit keelFastDocsKit = new KeelFastDocsKit(urlPathBase, docsDirPathBase)
                .setDocumentSubject(subject)
                .setFooterText(footer);

        router.route(urlPathBase + "*")
              .handler(keelFastDocsKit::processRouterRequest);
    }

    /**
     * Sets the document subject before this site starts serving requests.
     *
     * @param documentSubject document subject displayed in generated pages
     * @return this kit
     */
    public KeelFastDocsKit setDocumentSubject(String documentSubject) {
        this.documentSubject = documentSubject;
        return this;
    }

    /**
     * Sets the footer text for generated pages.
     *
     * @param footerText footer text displayed in generated pages
     * @return this kit
     */
    public KeelFastDocsKit setFooterText(String footerText) {
        this.footerText = footerText;
        return this;
    }

    public void processRouterRequest(RoutingContext ctx) {
        if (!Objects.equals(ctx.request().method(), HttpMethod.GET)) {
            ctx.response().setStatusCode(405).end();
            return;
        }

        String requestPath = ctx.request().path();

        PageBuilderOptions options = new PageBuilderOptions();
        options.ctx = ctx;
        options.subjectOfDocuments = this.documentSubject;
        options.footerText = this.footerText;
        options.rootURLPath = this.rootURLPath;
        options.rootMarkdownFilePath = this.rootMarkdownFilePath;

        if (requestPath.equals(rootURLPath) || requestPath.equals(rootURLPath + "/")) {
            ctx.redirect(rootURLPath + (rootURLPath.endsWith("/") ? "" : "/") + "index.md");
        } else if (requestPath.endsWith(".md")) {
            processRequestWithMarkdownPath(options);
        } else if (requestPath.equalsIgnoreCase(this.rootURLPath + "catalogue")) {
            processRequestWithCatalogue(options);
        } else if (requestPath.equalsIgnoreCase(this.rootURLPath + "markdown.css")) {
            processRequestWithMarkdownCSS(options);
        } else {
            processRequestWithStaticPath(options);
        }
    }

    private Future<String> getRelativePathOfRequest(RoutingContext ctx) {
        String requestPath = ctx.request().path();

        if (!requestPath.startsWith(this.rootURLPath)) {
            return Future.failedFuture("Not match url root");
        }
        try {
            var raw = requestPath.substring(this.rootURLPath.length());
            var decoded = FastDocsPathCodec.decodePath(raw);

            // Validate after decoding so encoded traversal cannot escape the resource root.
            Path normalized = Path.of(decoded).normalize();
            if (normalized.startsWith("..") || normalized.isAbsolute()) {
                return Future.failedFuture("Invalid path: traversal detected");
            }
            return Future.succeededFuture(normalized.toString());
        } catch (IllegalArgumentException e) {
            return Future.failedFuture(e);
        }
    }

    protected void processRequestWithMarkdownPath(PageBuilderOptions options) {
        getRelativePathOfRequest(options.ctx)
                .compose(relativePathOfMarkdownFile -> {
                    String markdownFilePath = this.rootMarkdownFilePath + relativePathOfMarkdownFile;
                    String markdownContent;
                    try (InputStream resourceAsStream = getClass().getClassLoader()
                                                                  .getResourceAsStream(markdownFilePath)) {
                        if (resourceAsStream == null) {
                            throw new IOException("resourceAsStream is null");
                        }
                        byte[] bytes = resourceAsStream.readAllBytes();
                        markdownContent = new String(bytes);
                    } catch (IOException e) {
                        return Future.failedFuture("Cannot read target file: " + e.getMessage());
                    }

                    options.markdownContent = markdownContent;

                    return Future.succeededFuture(new MarkdownPageBuilder(options));
                })
                .compose(MarkdownPageBuilder::respond)
                .onFailure(throwable -> {
                    if (!options.ctx.response().ended()) {
                        options.ctx.response().setStatusCode(404).end();
                    }
                });
    }

    protected void processRequestWithCatalogue(PageBuilderOptions options) {
        options.fromDoc = options.ctx.request().getParam("from_doc");
        String catalogueDivContent = getCatalogueDivContent(options);
        new CataloguePageBuilder(options, catalogueDivContent).respond()
                .onFailure(throwable -> {
                    if (!options.ctx.response().ended()) {
                        options.ctx.response().setStatusCode(500).end();
                    }
                });
    }

    /**
     * Returns the cached catalogue body for the requested display mode. The document tree
     * is read once per kit instance, while standalone and in-document HTML are rendered and
     * cached separately.
     *
     * @param options page options for the current catalogue request
     * @return cached or newly rendered catalogue HTML
     */
    String getCatalogueDivContent(PageBuilderOptions options) {
        boolean embeddedInDocument = options.fromDoc != null && !options.fromDoc.isEmpty();
        String cached = embeddedInDocument
                ? inDocumentCatalogueDivContentCache
                : standaloneCatalogueDivContentCache;
        if (cached != null) {
            return cached;
        }

        synchronized (catalogueCacheLock) {
            cached = embeddedInDocument
                    ? inDocumentCatalogueDivContentCache
                    : standaloneCatalogueDivContentCache;
            if (cached == null) {
                CataloguePageBuilder builder = new CataloguePageBuilder(options);
                CataloguePageBuilder.@Nullable TreeNode tree = catalogueTreeCache;
                if (tree == null) {
                    tree = builder.buildCatalogueTree();
                    catalogueTreeCache = tree;
                }
                cached = builder.buildCatalogueDivContent(tree);
                if (embeddedInDocument) {
                    inDocumentCatalogueDivContentCache = cached;
                } else {
                    standaloneCatalogueDivContentCache = cached;
                }
            }
            return cached;
        }
    }

    protected void processRequestWithMarkdownCSS(PageBuilderOptions options) {
        new MarkdownCssBuilder(options).respond()
                .onFailure(throwable -> {
                    if (!options.ctx.response().ended()) {
                        options.ctx.response().setStatusCode(500).end();
                    }
                });
    }

    protected void processRequestWithStaticPath(PageBuilderOptions options) {
        this.staticHandler.handle(options.ctx);
    }
}
