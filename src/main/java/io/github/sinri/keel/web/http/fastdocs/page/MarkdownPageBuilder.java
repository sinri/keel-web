package io.github.sinri.keel.web.http.fastdocs.page;

import io.github.sinri.keel.core.markdown.KeelMarkdownKit;
import io.github.sinri.keel.web.http.fastdocs.PageBuilderOptions;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * @since 5.0.0
 */
public class MarkdownPageBuilder implements FastDocsContentResponder {

    private final PageBuilderOptions options;

    public MarkdownPageBuilder(PageBuilderOptions options) {
        this.options = options;
    }

    protected String getPageTitle() {
        return options.subjectOfDocuments
                + " - "
                + URLDecoder.decode(options.ctx.request().path()
                                               .substring(this.options.rootURLPath.length()), StandardCharsets.UTF_8);
    }

    protected String getLogoDivContent() {
        return options.subjectOfDocuments;
    }

    protected String getComputedBreadcrumbDivContent() {
        String[] components = URLDecoder.decode(
                options.ctx.request().path().substring(this.options.rootURLPath.length()),
                StandardCharsets.UTF_8
        ).split("/");
        List<String> x = new ArrayList<>();
        StringBuilder href = new StringBuilder(this.options.rootURLPath);
        x.add("<a href='" + href + "index.md" + "'>" + options.subjectOfDocuments + "</a>");
        for (var component : components) {
            if (!href.toString().endsWith("/")) {
                href.append("/");
            }
            href.append(component);
            x.add("<a href='" + href + (component.endsWith(".md") ? "" : "/index.md") + "'>" + component + "</a>");
        }
        return String.join("&nbsp;‣&nbsp;", x);
    }

    protected String getFooterDivContent() {
        return options.footerText + " <div style=\"display: inline-block;color: gray;\">|</div> Powered by FastDocs";
    }

    private String getCatalogueLink(String fromDoc) {
        return this.options.rootURLPath + "catalogue" + (
                (fromDoc != null && !fromDoc.isEmpty())
                        ? ("?from_doc=" + fromDoc)
                        : ""
        );
    }

    protected String buildPage() {
        KeelMarkdownKit keelMarkdownKit = new KeelMarkdownKit();
        return """
               <!doctype html>
               <html lang="en">
               <head>
                   <meta name='viewport' content='width=device-width, initial-scale=1'>
                   <title>%s</title>
                   <link rel="stylesheet"
                         href="%smarkdown.css">
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
               
                           #parsed_md_div {
                               margin: 50px 10px 50px 300px;
                               padding: 10px;
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
                           }
                           #catalogue_div{
                               position: fixed;
                               left:0;
                               top:50px;
                               bottom: 50px;
                               width: 300px;
                               border-right: 1px solid gray;
                           }
                           #catalogue_iframe{
                               height: 100%%;
                               width: 300px;
                               border:none;
                           }
                       </style>
                   </head>
                   <body>
                       <div id="header_div">
                           <div style="display: inline-block;">%s</div>
                           <div style="display: inline-block;margin-left:50px;font-size: 10px;line-height: 22px">%s</div>
                       </div>
                       <div id='parsed_md_div' class='markdown-body'>
                           %s
                       </div>
                       <div id="footer_div">
                           <div style="display: inline-block;margin: auto 5px;">%s</div>
                           <div style="display: inline-block;color: gray;">|</div>
                           <div style="display: inline-block;margin: auto 5px;">
                               <a href="%s">Catalogue</a>
                           </div>
                       </div>
                       <div id="catalogue_div">
                           <iframe id="catalogue_iframe" name="catalogue_iframe"
                                   src="%s"
                           ></iframe>
                       </div>
                   </body>
               </html>""".formatted(getPageTitle(), this.options.rootURLPath, getLogoDivContent(), getComputedBreadcrumbDivContent(), keelMarkdownKit.convertMarkdownToHtml(options.markdownContent), getFooterDivContent(), getCatalogueLink(null), getCatalogueLink(options.ctx.request()
                                                                                                                                                                                                                                                                                 .path()
                                                                                                                                                                                                                                                                                 .substring(this.options.rootURLPath.length())));
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
}
