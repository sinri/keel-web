package io.github.sinri.keel.web.http.fastdocs.page;

import io.github.sinri.keel.core.utils.FileUtils;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;

/**
 * FastDocs Catalogue Page Builder
 *
 * @since 5.0.0
 */
public class CataloguePageBuilder implements FastDocsContentResponder {
    private static String catalogueDivContentCache = null;
    private final PageBuilderOptions options;
    private final boolean embedded;
    private final String actualFileRootOutsideJAR;

    public CataloguePageBuilder(PageBuilderOptions options) {
        this.options = options;

        URL x = getClass().getClassLoader().getResource(this.options.rootMarkdownFilePath);
        if (x == null) {
            throw new IllegalArgumentException("rootMarkdownFilePath is not available in File System");
        }
        this.embedded = x.toString().contains("!/");
        this.actualFileRootOutsideJAR = x.getPath();
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
        return options.subjectOfDocuments;
    }

    protected String getPageTitle() {
        return options.subjectOfDocuments
                + " - " +
                URLDecoder.decode(options.ctx.request().path()
                                             .substring(this.options.rootURLPath.length()), StandardCharsets.UTF_8);
    }

    protected String getCatalogueDivContent() {
        if (catalogueDivContentCache == null) {
            if (embedded) {
                catalogueDivContentCache = createHTMLCodeForDir(buildTreeInsideJAR()).toString();
            } else {
                catalogueDivContentCache = createHTMLCodeForDir(buildTreeOutsideJAR()).toString();
            }
        }
        return catalogueDivContentCache;
    }

    protected String getFooterDivContent() {
        return options.footerText + " | Powered by FastDocs";
    }

    public StringBuilder createHTMLCodeForDir(TreeNode tree) {
        StringBuilder sb = new StringBuilder();

        sb.append("<div class='dir_box'>");

        String boxHref;
        String displayDirName;
        boxHref = tree.href;
        if (tree.level > 0) {
            displayDirName = tree.name;
        } else {
            displayDirName = options.subjectOfDocuments;
        }

        sb.append("<div class='dir_box_body_item'>");
        sb.append("<div style='display: inline-block;width:20px;border-left: 1px solid lightgrey;'>&nbsp;</div>".repeat(Math.max(0, tree.level)));
        sb.append("<div class='dir_box_title' style='display: inline-block;'>")
          .append("<a href='").append(boxHref).append("' ").append(isFromDoc() ? "target='_parent'" : "")
          .append(" style='white-space: nowrap;display: inline-block;'").append(" >").append("\uD83D\uDCC1&nbsp;")
          .append(displayDirName).append("</a>")
          .append("</div>");
        sb.append("</div>");

        // DIRS start
        if (tree.href.endsWith("/index.md")) {
            // as dir
            for (var child : tree.getSortedChildren()) {
                if (child.href.endsWith("/index.md")) {
                    sb.append(createHTMLCodeForDir(child));
                } else {
                    sb.append("<div class='dir_box_body_item'>");
                    sb.append(("<div style='display: inline-block;width:20px;border-left: 1px solid lightgrey;" +
                            "'>&nbsp;" +
                            "</div>").repeat(Math.max(0, tree.level + 1)));
                    sb
                            .append("<a href='")
                            .append(child.href)
                            .append("' ")
                            .append(isFromDoc() ? "target='_parent'" : "")
                            .append(" style='white-space: nowrap;display: inline-block;'")
                            .append(" >").append("\uD83D\uDCC4&nbsp;").append(child.name)
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
        TreeNode tree = new TreeNode();
        tree.href = options.rootURLPath + "index.md";
        tree.level = 0;
        tree.name = options.subjectOfDocuments;
        List<JarEntry> jarEntries = FileUtils.traversalInRunningJar(options.rootMarkdownFilePath);
        for (var jarEntry : jarEntries) {
            TreeNode child = buildTreeNodeInJar(jarEntry);
            if (child != null) {
                tree.addChild(child);
            }
        }
        return tree;
    }

    private TreeNode buildTreeNodeInJar(JarEntry jarEntry) {
        TreeNode treeNode = new TreeNode();
        treeNode.name = String.valueOf(Path.of(jarEntry.getName()).getFileName());
        if (jarEntry.isDirectory()) {
            treeNode.href = jarEntry.getName().substring(options.rootMarkdownFilePath.length()) + "/index.md";
            treeNode.level = Path.of(treeNode.href).getNameCount() - 1;
            treeNode.href = (options.rootURLPath + treeNode.href).replaceAll("/+", "/");

            List<JarEntry> jarEntries = FileUtils.traversalInRunningJar(jarEntry.getName());
            for (var childJarEntry : jarEntries) {
                var x = buildTreeNodeInJar(childJarEntry);
                if (x != null) treeNode.addChild(x);
            }
        } else {
            var fileName = Path.of(jarEntry.getName()).getFileName().toString();
            if (fileName.equalsIgnoreCase("index.md")) {
                return null;
            }
            if (!fileName.endsWith(".md")) {
                return null;
            }
            treeNode.href = jarEntry.getName().substring(options.rootMarkdownFilePath.length());
            treeNode.level = Path.of(treeNode.href).getNameCount();
            treeNode.href = (options.rootURLPath + treeNode.href).replaceAll("/+", "/");
        }
        return treeNode;
    }

    protected TreeNode buildTreeOutsideJAR() {
        File root = new File(actualFileRootOutsideJAR);

        TreeNode tree = new TreeNode();
        tree.href = options.rootURLPath + "index.md";
        tree.name = options.subjectOfDocuments;
        tree.level = 0;

        if (root.isDirectory()) {
            File[] files = root.listFiles();
            if (files != null) {
                for (var file : files) {
                    var x = buildTreeNodeOutsideJar(file);
                    if (x != null) {
                        tree.addChild(x);
                    }
                }
            }
        }

        return tree;
    }

    private TreeNode buildTreeNodeOutsideJar(File item) {
        // options.eventLogger.debug(r -> r.message("buildTreeNodeOutsideJar " + item.getAbsolutePath()));
        String base = new File(actualFileRootOutsideJAR).getAbsolutePath();
        TreeNode treeNode = new TreeNode();
        String baseUrlPath = item.getAbsolutePath().substring(base.length());
        if (item.isDirectory()) {
            treeNode.href = (baseUrlPath + "/index.md");
            treeNode.level = Path.of(treeNode.href).getNameCount() - 1;
            treeNode.href = (options.rootURLPath + treeNode.href).replaceAll("/+", "/");

            File[] files = item.listFiles();
            if (files != null) {
                for (var file : files) {
                    var x = buildTreeNodeOutsideJar(file);
                    if (x != null) treeNode.addChild(x);
                }
            }
        } else {
            if (!item.getName().endsWith(".md")) {
                return null;
            }
            if (item.getName().equalsIgnoreCase("index.md")) {
                return null;
            }
            treeNode.href = (options.rootURLPath + baseUrlPath)
                    .replaceAll("/+", "/");
            treeNode.level = Path.of(treeNode.href).getNameCount();
        }
        treeNode.name = item.getName();
        return treeNode;
    }

    public static class TreeNode {
        private final List<TreeNode> _children = new ArrayList<>();
        public String href;
        public String name;
        public int level;

        public void addChild(TreeNode child) {
            _children.add(child);
        }

        public JsonObject toJsonObject() {
            var x = new JsonObject()
                    .put("href", href)
                    .put("name", name)
                    .put("level", level);
            JsonArray array = new JsonArray();
            for (var child : _children) {
                array.add(child.toJsonObject());
            }
            x.put("children", array);
            return x;
        }

        public List<TreeNode> getSortedChildren() {
            _children.sort(Comparator.comparing(o -> o.name));
            return _children;
        }
    }
}
