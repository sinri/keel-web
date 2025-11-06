package io.github.sinri.keel.web.http.requester.error;

import io.vertx.core.buffer.Buffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The exception that HTTP Response Status Code is not 200, or the response body is not in the expected format.
 *
 * @since 4.0.3
 */
public final class ReceivedUnexpectedFormatResponse extends ReceivedUnexpectedResponse {
    public ReceivedUnexpectedFormatResponse(@Nonnull String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        super(requestLabel, "received response with dody in unexpected format", responseStatusCode, responseBody);
    }
}
