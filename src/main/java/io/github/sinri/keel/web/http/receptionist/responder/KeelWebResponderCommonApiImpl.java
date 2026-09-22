package io.github.sinri.keel.web.http.receptionist.responder;

import io.github.sinri.keel.core.utils.value.ValueBox;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.github.sinri.keel.web.http.receptionist.ReceptionistSpecificLog;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;



/**
 * Web 请求响应器的一种特定实现。
 * <p>
 * 无论处理过程是否发生错误，返回格式为 JSON 对象。<br>
 * 当处理过程正常返回结果时，返回{@code {"code":"OK","data":{...}}};<br>
 * 否则，返回{@code {"code":"FAILED","data":{...}}}。
 *
 * @since 5.0.0
 */
@NullMarked
class KeelWebResponderCommonApiImpl extends AbstractKeelWebResponder<JsonObject> {

    public KeelWebResponderCommonApiImpl(RoutingContext routingContext, SpecificLogger<ReceptionistSpecificLog> issueRecorder) {
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
    public String contentTypeToRespond() {
        return "application/json";
    }

    @Override
    public void respondOnFailure(KeelWebApiError webApiError, @Nullable ValueBox<?> dataValueBox) {
        getLogger().error(log -> {
            log.extra().put("request_id", readRequestID());
            log.message("Web API request failed").exception(webApiError);
        });
        JsonObject resp = buildResponseBody(Code.FAILED, null)
                .put("throwable", new JsonObject()
                        .put("class", webApiError.getClass().getName())
                        .put("message", webApiError.getMessage()));
        String encoded;
        try {
            if (dataValueBox != null) {
                resp.put("data", new JsonObject().put("extra", dataValueBox.getNonNullValue()));
            }
            encoded = resp.encode();
        } catch (Throwable e) {
            getLogger().error(log -> {
                log.extra().put("request_id", readRequestID());
                log.message("Failed to render Web API error extra").exception(e);
            });
            resp.put("data", new JsonObject().put("extra_render_error", "Unable to render extra data"));
            encoded = resp.encode();
        }
        recordResponseVerbosely(resp);
        if (webApiError.getStatusCode() != 200) {
            getRoutingContext().response().setStatusCode(webApiError.getStatusCode());
        }
        getRoutingContext().response()
                .putHeader(HttpHeaders.CONTENT_TYPE, contentTypeToRespond())
                .end(encoded);
    }

    protected final JsonObject buildResponseBody(Code code, @Nullable JsonObject data) {
        return new JsonObject()
                .put("request_id", readRequestID())
                .put("code", code.name())
                .put("data", data);
    }

    public enum Code {
        OK, FAILED
    }
}
