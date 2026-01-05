package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


/**
 * 请求接待日志。
 *
 * @since 5.0.0
 */
@NullMarked
public final class ReceptionistSpecificLog extends SpecificLog<ReceptionistSpecificLog> {
    public static final String TopicReceptionist = "Receptionist";
    public static final String AttributeRequest = "request";
    public static final String AttributeResponse = "response";
    public static final String AttributeRespondInfo = "RespondInfo";

    public ReceptionistSpecificLog(String requestId) {
        super();
        this.extra("request_id", requestId);
    }

    public ReceptionistSpecificLog setRequest(
            HttpMethod method,
            String path,
            Class<?> receptionistClass,
            @Nullable String query,
            @Nullable String body
    ) {
        var x = new JsonObject()
                .put("method", method.name())
                .put("path", path)
                .put("handler", receptionistClass.getName());
        if (query != null) x.put("query", query);
        if (body != null) x.put("body", body);
        this.extra(AttributeRequest, x);
        return this;
    }

    public ReceptionistSpecificLog setResponse(@Nullable Object responseBody) {
        this.extra(AttributeResponse, new JsonObject()
                .put("body", responseBody)
        );
        return this;
    }

    public ReceptionistSpecificLog setRespondInfo(
            int statusCode,
            @Nullable String statusMessage,
            boolean ended,
            boolean closed
    ) {
        this.extra(AttributeRespondInfo, new JsonObject()
                .put("code", statusCode)
                .put("message", statusMessage)
                .put("ended", ended)
                .put("closed", closed)
        );
        return this;
    }
}
