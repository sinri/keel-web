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
 * To filter out the situation that Response Status Code is set and not 200, and the response body is not in JSON object
 * format, and the value mapped to key {@code code} is not {@code OK}.
 *
 * @since 4.0.3
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
