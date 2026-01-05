package io.github.sinri.keel.web.udp;

import io.github.sinri.keel.core.utils.BinaryUtils;
import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;


/**
 * 数据报相关的特定日志
 *
 * @since 3.2.0
 */
@NullMarked
public final class DatagramSpecificLog extends SpecificLog<DatagramSpecificLog> {
    public static final String TopicUdpDatagram = "UdpDatagram";

    public DatagramSpecificLog() {
        super();
    }


    private DatagramSpecificLog buffer(Buffer buffer, String address, int port, String action) {
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

    public DatagramSpecificLog bufferSent(Buffer buffer, String address, int port) {
        return this.buffer(buffer, address, port, "sent_to");
    }

    public DatagramSpecificLog bufferReceived(Buffer buffer, String address, int port) {
        return this.buffer(buffer, address, port, "received_from");
    }
}
