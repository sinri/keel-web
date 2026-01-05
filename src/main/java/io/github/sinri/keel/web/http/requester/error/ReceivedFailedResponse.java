package io.github.sinri.keel.web.http.requester.error;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


/**
 * 接收到的接口请求回复状态值不是 200，或回复报文的无法解析为一个 JSON 对象，或解析后的 code 字段的值不是 OK。
 *
 * @since 5.0.0
 */
@NullMarked
public final class ReceivedFailedResponse extends ReceivedUnexpectedResponse {
    public ReceivedFailedResponse(String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        super(requestLabel, "received response with code as Failed", responseStatusCode, responseBody);
    }

    public @Nullable JsonObject getResponseBodyAsJsonObject() {
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
