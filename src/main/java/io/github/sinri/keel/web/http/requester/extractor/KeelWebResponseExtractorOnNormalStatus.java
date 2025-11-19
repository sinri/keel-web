package io.github.sinri.keel.web.http.requester.extractor;

import io.github.sinri.keel.web.http.requester.error.ReceivedAbnormalStatusResponse;
import io.github.sinri.keel.web.http.requester.error.ReceivedUnexpectedResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * To filter out the situation that Response Status Code is set and not 200.
 *
 * @since 4.0.3
 */
public class KeelWebResponseExtractorOnNormalStatus extends KeelWebResponseExtractor<Buffer> {
    public KeelWebResponseExtractorOnNormalStatus(@NotNull String requestLabel, HttpResponse<Buffer> response) {
        super(requestLabel, response);
    }

    public KeelWebResponseExtractorOnNormalStatus(@NotNull String requestLabel, int responseStatusCode, @Nullable Buffer responseBody) {
        super(requestLabel, responseStatusCode, responseBody);
    }

    @Override
    @Nullable
    public Buffer extract() throws ReceivedUnexpectedResponse {
        var responseStatusCode = this.getResponseStatusCode();
        if (responseStatusCode != 200) {
            throw new ReceivedAbnormalStatusResponse(getRequestLabel(), responseStatusCode, this.getResponseBody());
        }
        return this.getResponseBody();
    }
}
