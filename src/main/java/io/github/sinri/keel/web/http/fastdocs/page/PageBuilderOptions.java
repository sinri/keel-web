package io.github.sinri.keel.web.http.fastdocs.page;

import io.vertx.ext.web.RoutingContext;

/**
 * @since 5.0.0
 */
public class PageBuilderOptions {
    public String rootURLPath;
    public String rootMarkdownFilePath;
    public String fromDoc;
    public RoutingContext ctx;
    public String markdownContent;

    public String subjectOfDocuments = "FastDocs";
    public String footerText = "Without Copyright";
}
