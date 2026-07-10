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


    private DatagramSpecificLog bufferSummary(Buffer buffer, String address, int port, String action) {
        this.context(action, new JsonObject()
                    .put("address", address)
                    .put("port", port)
            )
            .context("buffer", new JsonObject()
                    .put("buffer_size", buffer.length())
            );
        return this;
    }

    private DatagramSpecificLog bufferPayload(Buffer buffer, String address, int port, String action, int maxPayloadBytes) {
        int capturedBytes = Math.min(buffer.length(), maxPayloadBytes);
        Buffer captured = buffer.getBuffer(0, capturedBytes);
        bufferSummary(buffer, address, port, action);
        this.context("buffer", new JsonObject()
                .put("buffer_content", BinaryUtils.encodeHexWithUpperDigits(captured))
                .put("buffer_size", buffer.length())
                .put("captured_bytes", capturedBytes)
                .put("truncated", capturedBytes < buffer.length())
        );
        return this;
    }

    public DatagramSpecificLog bufferSentSummary(Buffer buffer, String address, int port) {
        return this.bufferSummary(buffer, address, port, "sent_to");
    }

    public DatagramSpecificLog bufferReceivedSummary(Buffer buffer, String address, int port) {
        return this.bufferSummary(buffer, address, port, "received_from");
    }

    /**
     * Records a safe sent-datagram summary without payload content.
     *
     * @param buffer sent buffer
     * @param address target address
     * @param port target port
     * @return this log
     */
    public DatagramSpecificLog bufferSent(Buffer buffer, String address, int port) {
        return bufferSentSummary(buffer, address, port);
    }

    /**
     * Records a safe received-datagram summary without payload content.
     *
     * @param buffer received buffer
     * @param address sender address
     * @param port sender port
     * @return this log
     */
    public DatagramSpecificLog bufferReceived(Buffer buffer, String address, int port) {
        return bufferReceivedSummary(buffer, address, port);
    }

    public DatagramSpecificLog bufferSentPayload(Buffer buffer, String address, int port, int maxPayloadBytes) {
        return this.bufferPayload(buffer, address, port, "sent_to", maxPayloadBytes);
    }

    public DatagramSpecificLog bufferReceivedPayload(Buffer buffer, String address, int port, int maxPayloadBytes) {
        return this.bufferPayload(buffer, address, port, "received_from", maxPayloadBytes);
    }
}
