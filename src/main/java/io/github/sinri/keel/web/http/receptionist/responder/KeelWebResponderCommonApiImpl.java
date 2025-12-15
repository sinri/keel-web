package io.github.sinri.keel.web.http.receptionist.responder;

import io.github.sinri.keel.base.json.JsonifiedThrowable;
import io.github.sinri.keel.core.utils.value.ValueBox;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.github.sinri.keel.web.http.receptionist.ReceptionistSpecificLog;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


/**
 * Web 请求响应器的一种特定实现。
 * <p>
 * 无论处理过程是否发生错误，返回格式为 JSON 对象。<br>
 * 当处理过程正常返回结果时，返回{@code {"code":"OK","data":{...}}};<br>
 * 否则，返回{@code {"code":"FAILED","data":{...}}}。
 *
 * @since 5.0.0
 */
class KeelWebResponderCommonApiImpl extends AbstractKeelWebResponder<JsonObject> {

    public KeelWebResponderCommonApiImpl(@NotNull RoutingContext routingContext, @NotNull SpecificLogger<ReceptionistSpecificLog> issueRecorder) {
        super(routingContext, issueRecorder);
    }

    @Override
    public void respondOnSuccess(@Nullable JsonObject data) {
        try {
            JsonObject resp = buildResponseBody(Code.OK, data);
            String encode = resp.encode();

            String contentTypeToRespond = this.contentTypeToRespond();
            getRoutingContext().response().putHeader(HttpHeaders.CONTENT_TYPE, contentTypeToRespond);
            getRoutingContext().response().end(encode);
        } catch (Throwable e) {
            respondOnFailure(KeelWebApiError.wrap(e));
        }
    }

    @Override
    @NotNull
    public String contentTypeToRespond() {
        return "application/json";
    }

    @Override
    public void respondOnFailure(@NotNull KeelWebApiError webApiError, @Nullable ValueBox<?> dataValueBox) {
        JsonObject resp;
        try {
            Objects.requireNonNull(dataValueBox);
            var v = dataValueBox.getNonNullValue();
            resp = buildResponseBody(Code.FAILED, new JsonObject().put("extra", v));
        } catch (Throwable e) {
            resp = buildResponseBody(Code.FAILED, new JsonObject().put("extra_render_error", webApiError.getMessage()));
        }
        resp.put("throwable", JsonifiedThrowable.wrap(webApiError).toJsonObject());
        recordResponseVerbosely(resp);
        if (webApiError.getStatusCode() != 200) {
            getRoutingContext().response().setStatusCode(webApiError.getStatusCode());
        }
        getRoutingContext().json(resp);
    }

    protected final JsonObject buildResponseBody(Code code, JsonObject data) {
        return new JsonObject()
                .put("request_id", readRequestID())
                .put("code", code.name())
                .put("data", data);
    }

    public enum Code {
        OK, FAILED
    }
}
