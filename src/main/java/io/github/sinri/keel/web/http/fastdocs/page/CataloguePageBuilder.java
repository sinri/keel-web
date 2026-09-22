package io.github.sinri.keel.web.http.fastdocs.page;

import io.github.sinri.keel.base.json.JsonObjectConvertible;
import io.github.sinri.keel.core.utils.FileUtils;
import io.github.sinri.keel.web.http.fastdocs.PageBuilderOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;

/**
 * FastDocs Catalogue Page Builder
 *
 * @since 5.0.0
 */
@NullMarked
public class CataloguePageBuilder implements FastDocsContentResponder {
    private final PageBuilderOptions options;
    private final boolean embedded;
    private final @Nullable Path actualFileRootOutsideJAR;
    private final @Nullable String catalogueDivContent;

    /**
     * Creates a catalogue page builder that generates its catalogue body on demand.
     *
     * @param options page options for the current request
     */
    public CataloguePageBuilder(PageBuilderOptions options) {
        this(options, null);
    }

    /**
     * Creates a catalogue page builder with optional pre-rendered catalogue content.
     *
     * @param options page options for the current request
     * @param catalogueDivContent pre-rendered catalogue body, or {@code null} to build it
     */
    public CataloguePageBuilder(PageBuilderOptions options, @Nullable String catalogueDivContent) {
        this.options = options;
        this.catalogueDivContent = catalogueDivContent;

        URL x = getClass().getClassLoader().getResource(this.options.rootMarkdownFilePath);
        if (x == null) {
            throw new IllegalArgumentException("rootMarkdownFilePath is not available in File System");
        }
        this.embedded = x.toString().contains("!/");
        try {
            this.actualFileRootOutsideJAR = embedded ? null : Path.of(x.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Markdown resource URI", e);
        }
    }

    @Override
    public void setRoutingContext(RoutingContext ctx) {
        this.options.ctx = ctx;
    }

    @Override
    public Future<Void> respond() {
        return this.options.ctx.response()
                               .putHeader("Content-Type", "text/html;charset=UTF-8")
                               .end(this.buildPage());
    }

    protected String buildPage() {
        return """
               <!doctype html>
               <html lang="en">
               <head>
                   <meta name='viewport' content='width=device-width, initial-scale=1'>
                   <title>%s</title>
                   <!--suppress HtmlUnknownTarget -->
                   <link rel="stylesheet" href="markdown.css">
                   <style>
                       body {
                           margin: 0;
                           background: white;
                       }
               
                       #header_div {
                           background-color: #dddddd;
                           padding: 10px;
                           height: 30px;
                           position: fixed;
                           top:0;
                           width: 100%%;
                           line-height: 30px;
                           %s
                       }
                       #header_div a:link{
                           text-decoration: none;
                           color: gray;
                       }
                       #header_div a:visited{
                           text-decoration: none;
                           color: gray;
                       }
                       #header_div a:hover{
                           text-decoration: none;
                           color: cornflowerblue;
                       }
                       #catalogue_div {
                           margin: %s;
                       }
                       #footer_div{
                           background-color: #dddddd;
                           text-align: center;
                           padding: 10px;
                           height: 30px;
                           width: 100%%;
                           position: fixed;
                           bottom: 0;
                           line-height: 30px;
                           %s
                       }
                       div.dir_box_body_item {
                           white-space: nowrap;
                       }
                       div.dir_box_body_item > * {
                           display: inline-block;
                           max-width: inherit;
                       }
                   </style>
               </head>
               <body>
                   <div id="header_div">
                       <div style="display: inline-block;">%s</div>
                   </div>
                   <div id='catalogue_div' class='markdown-body'>
                       %s
                   </div>
                   <div id="footer_div">
                       %s
                   </div>
                   <!--suppress JSUnusedGlobalSymbols -->
                   <script lang="JavaScript">
                       function locateParentToTargetPage(target) {
                           window.parent.window.location = target;
                       }
                   </script>
               </body>
               </html>"""
                .formatted(
                        getPageTitle(),
                        isFromDoc() ? "display: none;" : "",
                        isFromDoc() ? "10px" : "50px 10px 50px",
                        isFromDoc() ? "display: none;" : "",
                        getLogoDivContent(),
                        getCatalogueDivContent(),
                        getFooterDivContent()
                );
    }

    private boolean isFromDoc() {
        return options.fromDoc != null && !options.fromDoc.isEmpty();
    }

    protected String getLogoDivContent() {
        return HtmlEscaper.escape(options.subjectOfDocuments);
    }

    protected String getPageTitle() {
        return HtmlEscaper.escape(options.subjectOfDocuments
                + " - " +
                FastDocsPathCodec.decodePath(options.ctx.request().path()
                                             .substring(this.options.rootURLPath.length())));
    }

    protected String getCatalogueDivContent() {
        return Objects.requireNonNullElseGet(catalogueDivContent, this::buildCatalogueDivContent);
    }

    /**
     * Builds the catalogue body for the current options without applying a shared cache.
     *
     * @return generated catalogue HTML
     */
    public String buildCatalogueDivContent() {
        return buildCatalogueDivContent(buildCatalogueTree());
    }

    /**
     * Renders a previously built document tree as catalogue HTML.
     *
     * @param tree immutable document tree to render
     * @return rendered catalogue HTML
     */
    public String buildCatalogueDivContent(TreeNode tree) {
        return createHTMLCodeForDir(tree).toString();
    }

    /**
     * Reads the configured Markdown root and builds its immutable document tree.
     *
     * @return immutable document tree
     */
    public TreeNode buildCatalogueTree() {
        return embedded ? buildTreeInsideJAR() : buildTreeOutsideJAR();
    }

    protected String getFooterDivContent() {
        return HtmlEscaper.escape(options.footerText) + " | Powered by FastDocs";
    }

    public StringBuilder createHTMLCodeForDir(TreeNode tree) {
        StringBuilder sb = new StringBuilder();

        sb.append("<div class='dir_box'>");

        String boxHref;
        String displayDirName;
        boxHref = tree.href();
        if (tree.level() > 0) {
            displayDirName = tree.name();
        } else {
            displayDirName = options.subjectOfDocuments;
        }

        sb.append("<div class='dir_box_body_item'>");
        sb.append("<div style='display: inline-block;width:20px;border-left: 1px solid lightgrey;'>&nbsp;</div>".repeat(Math.max(0, tree.level())));
        sb.append("<div class='dir_box_title' style='display: inline-block;'>")
          .append("<a href='").append(HtmlEscaper.escape(boxHref)).append("' ")
          .append(isFromDoc() ? "target='_parent'" : "")
          .append(" style='white-space: nowrap;display: inline-block;'").append(" >").append("\uD83D\uDCC1&nbsp;")
          .append(HtmlEscaper.escape(displayDirName)).append("</a>")
          .append("</div>");
        sb.append("</div>");

        // DIRS start
        if (tree.href().endsWith("/index.md")) {
            // as dir
            for (var child : tree.children()) {
                if (child.href().endsWith("/index.md")) {
                    sb.append(createHTMLCodeForDir(child));
                } else {
                    sb.append("<div class='dir_box_body_item'>");
                    sb.append(("<div style='display: inline-block;width:20px;border-left: 1px solid lightgrey;" +
                            "'>&nbsp;" +
                            "</div>").repeat(Math.max(0, tree.level() + 1)));
                    sb
                            .append("<a href='")
                            .append(HtmlEscaper.escape(child.href()))
                            .append("' ")
                            .append(isFromDoc() ? "target='_parent'" : "")
                            .append(" style='white-space: nowrap;display: inline-block;'")
                            .append(" >").append("\uD83D\uDCC4&nbsp;").append(HtmlEscaper.escape(child.name()))
                            .append("</a>")
                            //                            .append("</span>")
                            .append("</div>");
                }
            }
        }
        // DIRS end

        sb.append("</div>");
        return sb;
    }

    protected TreeNode buildTreeInsideJAR() {
        List<TreeNode> children = new ArrayList<>();
        List<JarEntry> jarEntries = FileUtils.traversalInRunningJar(options.rootMarkdownFilePath);
        for (var jarEntry : jarEntries) {
            TreeNode child = buildTreeNodeInJar(jarEntry);
            if (child != null) {
                children.add(child);
            }
        }
        return new TreeNode(options.rootURLPath + "index.md", options.subjectOfDocuments, 0, children);
    }

    private @Nullable TreeNode buildTreeNodeInJar(JarEntry jarEntry) {
        String name = String.valueOf(Path.of(jarEntry.getName()).getFileName());
        String href;
        int level;
        List<TreeNode> children = new ArrayList<>();
        if (jarEntry.isDirectory()) {
            href = jarEntry.getName().substring(options.rootMarkdownFilePath.length()) + "/index.md";
            level = Path.of(href).getNameCount() - 1;
            href = (options.rootURLPath + FastDocsPathCodec.encodePath(href)).replaceAll("/+", "/");

            List<JarEntry> jarEntries = FileUtils.traversalInRunningJar(jarEntry.getName());
            for (var childJarEntry : jarEntries) {
                var x = buildTreeNodeInJar(childJarEntry);
                if (x != null) children.add(x);
            }
        } else {
            var fileName = Path.of(jarEntry.getName()).getFileName().toString();
            if (fileName.equalsIgnoreCase("index.md")) {
                return null;
            }
            if (!fileName.endsWith(".md")) {
                return null;
            }
            href = jarEntry.getName().substring(options.rootMarkdownFilePath.length());
            level = Path.of(href).getNameCount();
            href = (options.rootURLPath + FastDocsPathCodec.encodePath(href)).replaceAll("/+", "/");
        }
        return new TreeNode(href, name, level, children);
    }

    protected TreeNode buildTreeOutsideJAR() {
        File root = Objects.requireNonNull(actualFileRootOutsideJAR).toFile();

        List<TreeNode> children = new ArrayList<>();

        if (root.isDirectory()) {
            File[] files = root.listFiles();
            if (files != null) {
                for (var file : files) {
                    var x = buildTreeNodeOutsideJar(file);
                    if (x != null) {
                        children.add(x);
                    }
                }
            }
        }

        return new TreeNode(options.rootURLPath + "index.md", options.subjectOfDocuments, 0, children);
    }

    private @Nullable TreeNode buildTreeNodeOutsideJar(File item) {
        // options.eventLogger.debug(r -> r.message("buildTreeNodeOutsideJar " + item.getAbsolutePath()));
        String base = Objects.requireNonNull(actualFileRootOutsideJAR).toFile().getAbsolutePath();
        String baseUrlPath = item.getAbsolutePath().substring(base.length());
        String href;
        int level;
        List<TreeNode> children = new ArrayList<>();
        if (item.isDirectory()) {
            href = baseUrlPath + "/index.md";
            level = Path.of(href).getNameCount() - 1;
            href = (options.rootURLPath + FastDocsPathCodec.encodePath(href)).replaceAll("/+", "/");

            File[] files = item.listFiles();
            if (files != null) {
                for (var file : files) {
                    var x = buildTreeNodeOutsideJar(file);
                    if (x != null) children.add(x);
                }
            }
        } else {
            if (!item.getName().endsWith(".md")) {
                return null;
            }
            if (item.getName().equalsIgnoreCase("index.md")) {
                return null;
            }
            href = (options.rootURLPath + FastDocsPathCodec.encodePath(baseUrlPath))
                    .replaceAll("/+", "/");
            level = Path.of(href).getNameCount();
        }
        return new TreeNode(href, item.getName(), level, children);
    }

    /**
     * Immutable node in a FastDocs catalogue tree.
     *
     * @param href URL of the represented Markdown file or directory index
     * @param name display name of the node
     * @param level nesting level in the catalogue
     * @param children child nodes; copied, sorted by name, and made unmodifiable
     */
    public record TreeNode(String href, String name, int level, List<TreeNode> children)
            implements JsonObjectConvertible {
        public TreeNode {
            children = children.stream()
                               .sorted(Comparator.comparing(TreeNode::name))
                               .toList();
        }

        /**
         * Converts this node and all descendants to JSON.
         *
         * @return recursive JSON representation of this node
         */
        public JsonObject toJsonObject() {
            var x = new JsonObject()
                    .put("href", href)
                    .put("name", name)
                    .put("level", level);
            JsonArray array = new JsonArray();
            for (var child : children) {
                array.add(child.toJsonObject());
            }
            x.put("children", array);
            return x;
        }

        @Override
        public String toJsonExpression() {
            return toJsonObject().encode();
        }

        @Override
        public String toFormattedJsonExpression() {
            return toJsonObject().encodePrettily();
        }
    }
}
