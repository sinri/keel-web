package io.github.sinri.keel.web.http.receptionist.responder;

import io.github.sinri.keel.core.ValueBox;
import io.github.sinri.keel.core.json.JsonifiedThrowable;
import io.github.sinri.keel.logger.issue.recorder.KeelIssueRecorder;
import io.github.sinri.keel.web.http.receptionist.ReceptionistIssueRecord;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @since 4.0.4
 */
public class KeelWebResponderCommonApiImpl extends AbstractKeelWebResponder {

    public KeelWebResponderCommonApiImpl(@Nonnull RoutingContext routingContext, @Nonnull KeelIssueRecorder<ReceptionistIssueRecord> issueRecorder) {
        super(routingContext, issueRecorder);
    }

    @Override
    public void respondOnSuccess(@Nullable Object data) {
        JsonObject resp = buildResponseBody(Code.OK, data);
        getRoutingContext().json(resp);
    }

    @Override
    public void respondOnFailure(@Nonnull KeelWebApiError webApiError, @Nonnull ValueBox<?> dataValueBox) {
        JsonObject resp;
        if (dataValueBox.isValueAlreadySet()) {
            resp = buildResponseBody(Code.FAILED, dataValueBox.getValue());
        } else {
            resp = buildResponseBody(Code.FAILED, webApiError.getMessage());
        }
        resp.put("throwable", JsonifiedThrowable.wrap(webApiError).toJsonObject());
        recordResponseVerbosely(resp);
        if (webApiError.getStatusCode() != 200) {
            getRoutingContext().response().setStatusCode(webApiError.getStatusCode());
        }
        getRoutingContext().json(resp);
    }

    protected final JsonObject buildResponseBody(Code code, Object data) {
        return new JsonObject()
                .put("request_id", readRequestID())
                .put("code", code.name())
                .put("data", data);
    }

    public enum Code {
        OK, FAILED
    }
}
