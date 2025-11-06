package io.github.sinri.keel.web.http.requester.error;

import io.vertx.core.buffer.Buffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The exception that HTTP Response Status Code is not 200.
 *
 * @since 4.0.3
 */
public final class ReceivedAbnormalStatusResponse extends ReceivedUnexpectedResponse {

    public ReceivedAbnormalStatusResponse(@Nonnull String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        super(requestLabel, "received response with abnormal status code (non 200)", responseStatusCode, responseBody);
    }

    public ReceivedAbnormalStatusResponse(@Nonnull String requestLabel, int responseStatusCode) {
        this(requestLabel, responseStatusCode, null);
    }
}
