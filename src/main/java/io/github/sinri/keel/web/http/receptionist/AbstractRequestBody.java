package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.core.json.UnmodifiableJsonifiableEntityImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @since 3.0.1 JSON only.
 * @since 3.2.13 automatically support JSON and FORM.
 */
abstract public class AbstractRequestBody extends UnmodifiableJsonifiableEntityImpl {

    public AbstractRequestBody(@Nonnull RoutingContext routingContext) {
        super(parse(routingContext));
    }

    private static JsonObject parse(@Nonnull RoutingContext routingContext) {
        Objects.requireNonNull(routingContext);
        String contentType = routingContext.request().headers().get("Content-Type");
        if (contentType != null) {
            if (contentType.contains("multipart/form-data")) {
                routingContext.request().setExpectMultipart(true);
            }
            if (contentType.contains("application/json")) {
                return routingContext.body().asJsonObject();
            }
        }
        JsonObject requestObject = new JsonObject();
        routingContext.request().formAttributes()
                      .forEach(entry -> requestObject.put(entry.getKey(), entry.getValue()));
        return requestObject;
    }
}
