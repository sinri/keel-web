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
    private String address = "0.0.0.0";
    private BiConsumer<SocketAddress, Buffer> datagramSocketConsumer = (sender, buffer) -> {
        // do nothing
    };

    public KeelUDPTransceiver(
            DatagramSocket udpServer,
            int port,
            String address,
            LoggerFactory loggerFactory
    ) {
        this.port = port;
        this.address = address;
        this.udpServer = udpServer;
        this.logger = loggerFactory.createLogger(DatagramSpecificLog.TopicUdpDatagram, DatagramSpecificLog::new);
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
                                                      .bufferReceived(data, sender.hostAddress(), sender.port())
                                              );
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
                        .onSuccess(done -> getLogger().info(r -> r.bufferSent(buffer, targetAddress, targetPort)))
                        .onFailure(throwable -> getLogger().error(x -> x.exception(throwable)
                                                                        .message("failed to send to " + targetAddress + ":" + targetPort)));
    }

    public Future<Void> close() {
        return udpServer.close()
                        .onSuccess(v -> getLogger().info(r -> r.message("closed")))
                        .onFailure(throwable -> getLogger().error(x -> x.exception(throwable)
                                                                        .message("failed to close")));
    }

    @Override
    public void close(Completable<Void> completion) {
        close().onComplete(completion);
    }
}
