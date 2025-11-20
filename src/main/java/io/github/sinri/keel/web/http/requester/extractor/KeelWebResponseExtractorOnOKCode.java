package io.github.sinri.keel.web.http.requester.extractor;

import io.github.sinri.keel.web.http.requester.error.ReceivedFailedResponse;
import io.github.sinri.keel.web.http.requester.error.ReceivedUnexpectedResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 验证接口请求回复报文的有效性并从中萃取出 JSON 对象，并在萃取过程中验证 code 字段为 OK。
 *
 * @since 5.0.0
 */
public class KeelWebResponseExtractorOnOKCode extends KeelWebResponseExtractorOnJsonObjectFormat {
    public KeelWebResponseExtractorOnOKCode(@NotNull String requestLabel, HttpResponse<Buffer> response) {
        super(requestLabel, response);
    }

    public KeelWebResponseExtractorOnOKCode(@NotNull String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        super(requestLabel, responseStatusCode, responseBody);
    }

    @NotNull
    @Override
    public JsonObject extract() throws ReceivedUnexpectedResponse {
        JsonObject j = super.extract();
        String code = j.getString("code");
        if (!Objects.equals("OK", code)) {
            throw new ReceivedFailedResponse(getRequestLabel(), getResponseStatusCode(), getResponseBody());
        }
        return j;
    }
}
