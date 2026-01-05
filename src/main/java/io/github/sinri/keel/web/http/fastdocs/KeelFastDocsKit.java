package io.github.sinri.keel.web.http.fastdocs;

import io.github.sinri.keel.web.http.fastdocs.page.CataloguePageBuilder;
import io.github.sinri.keel.web.http.fastdocs.page.MarkdownCssBuilder;
import io.github.sinri.keel.web.http.fastdocs.page.MarkdownPageBuilder;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    private String documentSubject = "FastDocs";
    private String footerText = "Public Domain";

    /**
     * @param rootURLPath          such as `/prefix/`
     * @param rootMarkdownFilePath such as `path/to/dir/`
     */
    public KeelFastDocsKit(String rootURLPath, String rootMarkdownFilePath) {
        this.staticHandler = StaticHandler.create();
        this.rootURLPath = rootURLPath;
        this.rootMarkdownFilePath = rootMarkdownFilePath;
    }

    /**
     * If you want to install a route for FastDocs in a certain Router, which mounts URL
     * `[schema]://[domain]/fast-docs/*` to the directory contains markdown files in `resources` as `webroot/markdown/*`
     * .
     *
     * @param router          Router
     * @param urlPathBase     such as `/fast-docs/`
     * @param docsDirPathBase such as `webroot/markdown/`
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
        if (!docsDirPathBase.endsWith("/")) {
            docsDirPathBase = docsDirPathBase + "/";
        }

        KeelFastDocsKit keelFastDocsKit = new KeelFastDocsKit(urlPathBase, docsDirPathBase)
                .setDocumentSubject(subject)
                .setFooterText(footer);

        router.route(urlPathBase + "*")
              .handler(keelFastDocsKit::processRouterRequest);
    }

    public KeelFastDocsKit setDocumentSubject(String documentSubject) {
        this.documentSubject = documentSubject;
        return this;
    }

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
        var raw = requestPath.substring(this.rootURLPath.length());
        return Future.succeededFuture(URLDecoder.decode(raw, StandardCharsets.UTF_8));
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
                .onFailure(throwable -> {
                    options.ctx.response().setStatusCode(404).end();
                })
                .compose(MarkdownPageBuilder::respond)
                .compose(v -> {
                    return Future.succeededFuture();
                });

    }

    protected void processRequestWithCatalogue(PageBuilderOptions options) {
        options.fromDoc = options.ctx.request().getParam("from_doc");
        new CataloguePageBuilder(options).respond()
                                         .compose(v -> {
                                             return Future.succeededFuture();
                                         });
    }

    protected void processRequestWithMarkdownCSS(PageBuilderOptions options) {
        new MarkdownCssBuilder(options).respond()
                                       .compose(v -> {
                                           return Future.succeededFuture();
                                       });
    }

    protected void processRequestWithStaticPath(PageBuilderOptions options) {
        this.staticHandler.handle(options.ctx);
    }
}
