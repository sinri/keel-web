package io.github.sinri.keel.web.http.requester.error;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * The exception that HTTP Response Status Code is not 200, or the response body is not a JSON object, or the field
 * {@code code} of the parsed body entity is not {@code OK}.
 *
 * @since 4.0.3
 */
public final class ReceivedFailedResponse extends ReceivedUnexpectedResponse {
    public ReceivedFailedResponse(@NotNull String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        super(requestLabel, "received response with code as Failed", responseStatusCode, responseBody);
    }

    @Nullable
    public JsonObject getResponseBodyAsJsonObject() {
        Buffer responseBody = getResponseBody();
        if (responseBody == null) {
            return null;
        }
        try {
            return responseBody.toJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject x = super.toJsonObject();
        JsonObject j = getResponseBodyAsJsonObject();
        if (j != null) {
            x.getJsonObject("response").put("body", j);
        }
        return x;
    }
}
