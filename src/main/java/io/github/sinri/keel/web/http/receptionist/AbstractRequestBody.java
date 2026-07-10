package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.base.json.UnmodifiableJsonifiableEntityImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.RoutingContext;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

/**
 * 自待处理请求对象{@link RoutingContext}中抽取请求体并进行读取。
 * <p>
 * 请求对象的 Content-Type 可以是 JSON 或 FORM（包括分块传输）。使用本类前，请求应当已经经过
 * Vert.x {@link io.vertx.ext.web.handler.BodyHandler} 处理。
 * <p>
 * 对于 multipart/form-data，本类只抽取普通表单字段。上传的文件不属于该 JSON 对象，调用方应通过
 * {@link RoutingContext#fileUploads()} 读取。
 *
 * @since 5.0.0
 */
@NullMarked
abstract public class AbstractRequestBody extends UnmodifiableJsonifiableEntityImpl {

    public AbstractRequestBody(RoutingContext routingContext) {
        super(parse(routingContext));
    }

    private static JsonObject parse(RoutingContext routingContext) {
        Objects.requireNonNull(routingContext);
        MIMEHeader contentType = routingContext.parsedHeaders().contentType();
        if (contentType != null && "application/json".equalsIgnoreCase(contentType.value())) {
            return routingContext.body().asJsonObject();
        }
        // 表单提交的默认编码方式 application/x-www-form-urlencoded
        JsonObject requestObject = new JsonObject();
        routingContext.request().formAttributes()
                      .forEach(entry -> requestObject.put(entry.getKey(), entry.getValue()));
        return requestObject;
    }
}
