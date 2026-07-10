package io.github.sinri.keel.web.udp;

import io.github.sinri.keel.logger.api.factory.LoggerFactory;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.vertx.core.Closeable;
import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.net.SocketAddress;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * UDP 传输器
 *
 * @since 5.0.0
 */
@NullMarked
public class KeelUDPTransceiver implements Closeable {
    private final int port;
    private final DatagramSocket udpServer;
    private final SpecificLogger<DatagramSpecificLog> logger;
    private boolean debugPayloadLoggingEnabled;
    private int debugPayloadMaxBytes = 256;
    private String address = "0.0.0.0";
    private BiConsumer<SocketAddress, Buffer> datagramSocketConsumer = (sender, buffer) -> {
        // do nothing
    };

    public KeelUDPTransceiver(
            DatagramSocket udpServer,
            int port,
            String address
    ) {
        this.port = port;
        this.address = address;
        this.udpServer = udpServer;
        this.logger = LoggerFactory.getShared()
                                   .createLogger(DatagramSpecificLog.TopicUdpDatagram, DatagramSpecificLog::new);
    }

    public SpecificLogger<DatagramSpecificLog> getLogger() {
        return logger;
    }

    public KeelUDPTransceiver setDatagramSocketConsumer(BiConsumer<SocketAddress, Buffer> datagramSocketConsumer) {
        Objects.requireNonNull(datagramSocketConsumer);
        this.datagramSocketConsumer = datagramSocketConsumer;
        return this;
    }

    public Future<Object> listen() {
        return udpServer.listen(port, address)
                        .compose(datagramSocket -> {
                            datagramSocket.handler(datagramPacket -> {
                                              SocketAddress sender = datagramPacket.sender();
                                              Buffer data = datagramPacket.data();

                                              getLogger().info(r -> r
                                                      .bufferReceivedSummary(data, sender.hostAddress(), sender.port())
                                              );
                                              if (debugPayloadLoggingEnabled) {
                                                  getLogger().debug(r -> r.bufferReceivedPayload(
                                                          data, sender.hostAddress(), sender.port(), debugPayloadMaxBytes));
                                              }
                                              this.datagramSocketConsumer.accept(sender, data);
                                          })
                                          //.endHandler(end -> getIssueRecorder().info(r -> r.message("read end")))
                                          .exceptionHandler(throwable -> getLogger()
                                                  .error(x -> x.exception(throwable).message("read error")));
                            return Future.succeededFuture();
                        });
    }

    public Future<Void> send(Buffer buffer, int targetPort, String targetAddress) {
        return udpServer.send(buffer, targetPort, targetAddress)
                        .onSuccess(done -> {
                            getLogger().info(r -> r.bufferSentSummary(buffer, targetAddress, targetPort));
                            if (debugPayloadLoggingEnabled) {
                                getLogger().debug(r -> r.bufferSentPayload(
                                        buffer, targetAddress, targetPort, debugPayloadMaxBytes));
                            }
                        })
                        .onFailure(throwable -> getLogger().error(x -> x.exception(throwable)
                                                                        .message("failed to send to " + targetAddress + ":" + targetPort)));
    }

    public Future<Void> close() {
        return udpServer.close()
                        .onSuccess(v -> getLogger().info(r -> r.message("closed")))
                        .onFailure(throwable -> getLogger().error(x -> x.exception(throwable)
                                                                        .message("failed to close")));
    }

    /**
     * Enables or disables payload logging at DEBUG level. Payload logging is disabled by default.
     *
     * @param enabled whether payload logging is enabled
     * @return this transceiver
     */
    public KeelUDPTransceiver setDebugPayloadLoggingEnabled(boolean enabled) {
        this.debugPayloadLoggingEnabled = enabled;
        return this;
    }

    /**
     * Sets the maximum number of payload bytes captured in each DEBUG log entry.
     *
     * @param maxBytes positive maximum payload size
     * @return this transceiver
     */
    public KeelUDPTransceiver setDebugPayloadMaxBytes(int maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        this.debugPayloadMaxBytes = maxBytes;
        return this;
    }

    @Override
    public void close(Completable<Void> completion) {
        close().onComplete(completion);
    }
}
