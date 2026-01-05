package io.github.sinri.keel.web.http.requester.extractor;

import io.github.sinri.keel.web.http.requester.error.ReceivedAbnormalStatusResponse;
import io.github.sinri.keel.web.http.requester.error.ReceivedUnexpectedFormatResponse;
import io.github.sinri.keel.web.http.requester.error.ReceivedUnexpectedResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 验证接口请求回复报文的有效性并从中萃取出 JSON 对象。
 *
 * @since 5.0.0
 */
@NullMarked
public class KeelWebResponseExtractorOnJsonObjectFormat extends KeelWebResponseExtractor<JsonObject> {
    public KeelWebResponseExtractorOnJsonObjectFormat(String requestLabel, HttpResponse<Buffer> response) {
        super(requestLabel, response);
    }

    public KeelWebResponseExtractorOnJsonObjectFormat(String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        super(requestLabel, responseStatusCode, responseBody);
    }

    @Override
    public JsonObject extract() throws ReceivedUnexpectedResponse {
        var responseStatusCode = getResponseStatusCode();
        var responseBody = this.getResponseBody();

        if (responseStatusCode != 200) {
            throw new ReceivedAbnormalStatusResponse(getRequestLabel(), responseStatusCode, responseBody);
        }

        try {
            Objects.requireNonNull(responseBody);
            return responseBody.toJsonObject();
        } catch (Exception e) {
            throw new ReceivedUnexpectedFormatResponse(getRequestLabel(), responseStatusCode, responseBody);
        }
    }
}
