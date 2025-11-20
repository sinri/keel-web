package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.base.json.UnmodifiableJsonifiableEntityImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 自待处理请求对象{@link RoutingContext}中抽取请求体并进行读取。
 * <p>
 * 请求对象的 Content Type 可以是 JSON 和 FORM（包括分块支持）。
 * @since 5.0.0
 */
abstract public class AbstractRequestBody extends UnmodifiableJsonifiableEntityImpl {

    public AbstractRequestBody(@NotNull RoutingContext routingContext) {
        super(parse(routingContext));
    }

    private static JsonObject parse(@NotNull RoutingContext routingContext) {
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
        // 表单提交的默认编码方式 application/x-www-form-urlencoded
        JsonObject requestObject = new JsonObject();
        routingContext.request().formAttributes()
                      .forEach(entry -> requestObject.put(entry.getKey(), entry.getValue()));
        return requestObject;
    }
}
