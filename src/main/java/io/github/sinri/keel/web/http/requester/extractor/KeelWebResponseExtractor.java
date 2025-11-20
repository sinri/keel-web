package io.github.sinri.keel.web.http.requester.extractor;

import io.github.sinri.keel.web.http.requester.error.ReceivedUnexpectedResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * 接口请求回复报文萃取器。
 *
 * @param <T> 从接口请求回复报文中萃取的值的类型
 * @since 5.0.0
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

    /**
     * 从接口请求回复报文中萃取值。
     *
     * @return 从接口请求回复报文中萃取的值
     * @throws ReceivedUnexpectedResponse 接收到的接口回复报文不符合预期，导致无法萃取时抛出此异常。
     */
    public abstract T extract() throws ReceivedUnexpectedResponse;
}
