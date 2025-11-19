package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * @since 3.2.0
 */
public final class ReceptionistIssueRecord extends SpecificLog<ReceptionistIssueRecord> {
    public static final String TopicReceptionist = "Receptionist";
    public static final String AttributeRequest = "request";
    public static final String AttributeResponse = "response";
    public static final String AttributeRespondInfo = "RespondInfo";

    public ReceptionistIssueRecord(@NotNull String requestId) {
        super();
        this.extra("request_id", requestId);
    }

    public ReceptionistIssueRecord setRequest(
            @NotNull HttpMethod method,
            @NotNull String path,
            @NotNull Class<?> receptionistClass,
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

    public ReceptionistIssueRecord setResponse(@Nullable Object responseBody) {
        this.extra(AttributeResponse, new JsonObject()
                .put("body", responseBody)
        );
        return this;
    }

    public ReceptionistIssueRecord setRespondInfo(
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
