package io.github.sinri.keel.web.http.requester.error;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The exception that HTTP Response is not expected.
 * <p>Detailed implementation classes of this exception class should keep
 * the constructor without any parameters and be public.</p>
 *
 * @since 4.0.3
 */
public class ReceivedUnexpectedResponse extends RuntimeException {
    private final int responseStatusCode;
    private final @Nullable Buffer responseBody;

    public ReceivedUnexpectedResponse(@Nonnull String requestLabel, @Nonnull String message, int responseStatusCode, @Nullable Buffer responseBody) {
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
