package io.github.sinri.keel.web.http.fastdocs.page;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

/**
 * @since 5.0.0
 */
public interface FastDocsContentResponder {
    void setRoutingContext(RoutingContext ctx);

    Future<Void> respond();
}
