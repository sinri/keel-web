package io.github.sinri.keel.web.http.requester.error;

import io.vertx.core.buffer.Buffer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


/**
 * 接收到的接口回复报文的格式不符合预期。
 *
 * @since 5.0.0
 */
@NullMarked
public final class ReceivedUnexpectedFormatResponse extends ReceivedUnexpectedResponse {
    public ReceivedUnexpectedFormatResponse(String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        super(requestLabel, "received response with body in unexpected format", responseStatusCode, responseBody);
    }
}
