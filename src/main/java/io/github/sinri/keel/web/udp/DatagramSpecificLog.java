package io.github.sinri.keel.web.udp;

import io.github.sinri.keel.core.utils.BinaryUtils;
import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;


/**
 * 数据报相关的特定日志
 *
 * @since 3.2.0
 */
public final class DatagramSpecificLog extends SpecificLog<DatagramSpecificLog> {
    public static final String TopicUdpDatagram = "UdpDatagram";

    public DatagramSpecificLog() {
        super();
    }


    private DatagramSpecificLog buffer(@NotNull Buffer buffer, @NotNull String address, int port, @NotNull String action) {
        this.context(action, new JsonObject()
                    .put("address", address)
                    .put("port", port)
            )
            .context("buffer", new JsonObject()
                    .put("buffer_content", BinaryUtils.encodeHexWithUpperDigits(buffer))
                    .put("buffer_size", buffer.length())
            );
        return this;
    }

    public DatagramSpecificLog bufferSent(@NotNull Buffer buffer, @NotNull String address, int port) {
        return this.buffer(buffer, address, port, "sent_to");
    }

    public DatagramSpecificLog bufferReceived(@NotNull Buffer buffer, @NotNull String address, int port) {
        return this.buffer(buffer, address, port, "received_from");
    }
}
