package io.github.sinri.keel.web.tcp;

import io.github.sinri.keel.core.utils.BinaryUtils;
import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;


/**
 * Socket 相关的特定日志。
 *
 * @since 5.0.0
 */
@NullMarked
public final class SocketSpecificLog extends SpecificLog<SocketSpecificLog> {
    public static final String TopicTcpSocket = "TcpSocket";

    public SocketSpecificLog() {
        super();
    }

    public SocketSpecificLog bufferSummary(Buffer buffer) {
        this.context("buffer", new JsonObject()
                .put("buffer_size", buffer.length())
        );
        return this;
    }

    /**
     * Records a safe buffer summary without payload content.
     *
     * @param buffer source buffer
     * @return this log
     */
    public SocketSpecificLog buffer(Buffer buffer) {
        return bufferSummary(buffer);
    }

    public SocketSpecificLog bufferPayload(Buffer buffer, int maxPayloadBytes) {
        int capturedBytes = Math.min(buffer.length(), maxPayloadBytes);
        Buffer captured = buffer.getBuffer(0, capturedBytes);
        this.context("buffer", new JsonObject()
                .put("buffer_content", BinaryUtils.encodeHexWithUpperDigits(captured))
                .put("buffer_size", buffer.length())
                .put("captured_bytes", capturedBytes)
                .put("truncated", capturedBytes < buffer.length())
        );
        return this;
    }
}
