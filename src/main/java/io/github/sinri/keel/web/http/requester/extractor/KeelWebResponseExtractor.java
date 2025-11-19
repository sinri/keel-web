package io.github.sinri.keel.web.http.requester.extractor;

import io.github.sinri.keel.web.http.requester.error.ReceivedUnexpectedResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Validate the response of web requests and extract the content if validated.
 * <p>
 * As of 4.1.4, the fields are changed.
 *
 * @since 4.0.3
 */
public abstract class KeelWebResponseExtractor<T> {
    @NotNull
    private final String requestLabel;
    private final int responseStatusCode;
    private final @Nullable Buffer responseBody;

    public KeelWebResponseExtractor(@NotNull String requestLabel, @NotNull HttpResponse<Buffer> response) {
        this(requestLabel, response.statusCode(), response.body());
    }

    public KeelWebResponseExtractor(@NotNull String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        this.requestLabel = requestLabel;
        this.responseStatusCode = responseStatusCode;
        this.responseBody = responseBody;
    }

    public int getResponseStatusCode() {
        return responseStatusCode;
    }


    @NotNull
    public String getRequestLabel() {
        return requestLabel;
    }

    @Nullable
    public Buffer getResponseBody() {
        return responseBody;
    }


    public abstract T extract() throws ReceivedUnexpectedResponse;
}
