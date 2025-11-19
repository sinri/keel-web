package io.github.sinri.keel.web.http.requester.error;

import io.vertx.core.buffer.Buffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * The exception that HTTP Response Status Code is not 200.
 *
 * @since 4.0.3
 */
public final class ReceivedAbnormalStatusResponse extends ReceivedUnexpectedResponse {

    public ReceivedAbnormalStatusResponse(@NotNull String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        super(requestLabel, "received response with abnormal status code (non 200)", responseStatusCode, responseBody);
    }

    public ReceivedAbnormalStatusResponse(@NotNull String requestLabel, int responseStatusCode) {
        this(requestLabel, responseStatusCode, null);
    }
}
