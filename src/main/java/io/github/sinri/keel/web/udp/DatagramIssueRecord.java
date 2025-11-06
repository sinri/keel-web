package io.github.sinri.keel.web.udp;

import io.github.sinri.keel.logger.issue.record.KeelIssueRecord;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

/**
 * @since 3.2.0
 */
public final class DatagramIssueRecord extends KeelIssueRecord<DatagramIssueRecord> {
    public static final String TopicUdpDatagram = "UdpDatagram";

    public DatagramIssueRecord() {
        super();
    }

    @Nonnull
    @Override
    public DatagramIssueRecord getImplementation() {
        return this;
    }

    private DatagramIssueRecord buffer(@Nonnull Buffer buffer, @Nonnull String address, int port, @Nonnull String action) {
        this.context(action, new JsonObject()
                    .put("address", address)
                    .put("port", port)
            )
            .context("buffer", new JsonObject()
                    .put("buffer_content", Keel.binaryHelper().encodeHexWithUpperDigits(buffer))
                    .put("buffer_size", buffer.length())
            );
        return this;
    }

    public DatagramIssueRecord bufferSent(@Nonnull Buffer buffer, @Nonnull String address, int port) {
        return this.buffer(buffer, address, port, "sent_to");
    }

    public DatagramIssueRecord bufferReceived(@Nonnull Buffer buffer, @Nonnull String address, int port) {
        return this.buffer(buffer, address, port, "received_from");
    }
}
