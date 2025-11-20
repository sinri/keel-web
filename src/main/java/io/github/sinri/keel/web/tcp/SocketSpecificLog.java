package io.github.sinri.keel.web.tcp;

import io.github.sinri.keel.core.utils.BinaryUtils;
import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;


/**
 * Socket 相关的特定日志。
 *
 * @since 5.0.0
 */
public final class SocketSpecificLog extends SpecificLog<SocketSpecificLog> {
    public static final String TopicTcpSocket = "TcpSocket";

    public SocketSpecificLog() {
        super();
    }

    public SocketSpecificLog buffer(@NotNull Buffer buffer) {
        this.context("buffer", new JsonObject()
                .put("buffer_content", BinaryUtils.encodeHexWithUpperDigits(buffer))
                .put("buffer_size", buffer.length())
        );
        return this;
    }
}
