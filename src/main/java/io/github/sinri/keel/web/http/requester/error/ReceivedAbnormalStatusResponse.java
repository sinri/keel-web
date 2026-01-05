package io.github.sinri.keel.web.http.requester.error;

import io.vertx.core.buffer.Buffer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


/**
 * 接收到的接口请求回复状态值不是 200。
 *
 * @since 5.0.0
 */
@NullMarked
public final class ReceivedAbnormalStatusResponse extends ReceivedUnexpectedResponse {

    public ReceivedAbnormalStatusResponse(String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        super(requestLabel, "received response with abnormal status code (non 200)", responseStatusCode, responseBody);
    }

    public ReceivedAbnormalStatusResponse(String requestLabel, int responseStatusCode) {
        this(requestLabel, responseStatusCode, null);
    }
}
