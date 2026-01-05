package io.github.sinri.keel.web.http.requester.error;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 接收到的接口回复报文不符合预期。
 *
 * @since 5.0.0
 */
@NullMarked
public class ReceivedUnexpectedResponse extends RuntimeException {
    private final int responseStatusCode;
    private final @Nullable Buffer responseBody;

    public ReceivedUnexpectedResponse(String requestLabel, String message, int responseStatusCode, @Nullable Buffer responseBody) {
        super("{" + requestLabel + "} " + message);
        this.responseStatusCode = responseStatusCode;
        this.responseBody = responseBody;
    }

    @Nullable
    public Integer getResponseStatusCode() {
        return responseStatusCode;
    }

    @Nullable
    public Buffer getResponseBody() {
        return responseBody;
    }

    public JsonObject toJsonObject() {
        return new JsonObject()
                .put("error", this.getClass().getName())
                .put("message", getMessage())
                .put("response", new JsonObject()
                        .put("status_code", getResponseStatusCode())
                        .put("body", getResponseBody())
                );
    }

    @Override
    public String toString() {
        return toJsonObject().toString();
    }
}
