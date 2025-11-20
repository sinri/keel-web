package io.github.sinri.keel.web.tcp;

import io.github.sinri.keel.core.servant.funnel.KeelFunnel;
import io.github.sinri.keel.logger.api.factory.LoggerFactory;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

import java.util.List;
import java.util.UUID;

import static io.github.sinri.keel.base.KeelInstance.Keel;

/**
 * 针对{@link NetSocket}的封装类。
 *
 * @since 5.0.0
 */
abstract public class KeelAbstractSocketWrapper {
    private final String socketID;
    private final NetSocket socket;
    private final KeelFunnel funnel;
    private final SpecificLogger<SocketSpecificLog> logger;

    public KeelAbstractSocketWrapper(NetSocket socket) {
        this(socket, UUID.randomUUID().toString());
    }

    public KeelAbstractSocketWrapper(NetSocket socket, String socketID) {
        this.socketID = socketID;
        this.socket = socket;

        this.logger = this.buildLogger();

        this.funnel = new KeelFunnel();
        this.funnel.deployMe(new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER));

        this.socket
                .handler(buffer -> {
                    getLogger().info(eventLog -> eventLog
                            .message("READ BUFFER " + buffer.length() + " BYTES")
                            .buffer(buffer)
                    );

                    this.funnel.add(() -> whenBufferComes(buffer));
                })
                .endHandler(end -> {
                    /*
                     Set an end handler.
                     Once the stream has ended, and there is no more data to be read,
                     this handler will be called.
                     This handler might be called after the close handler
                     when the socket is paused and there are still buffers to deliver.
                     */
                    getLogger().info(r -> r.message("READ TO END"));
                    whenReadToEnd();
                })
                .drainHandler(drain -> {
                    /*
                    Set a drain handler on the stream.
                    If the write queue is full,
                    then the handler will be called when the write queue is ready to accept buffers again.
                    See Pipe for an example of this being used.
                    The stream implementation defines when the drain handler,
                    for example it could be when the queue size has been reduced to maxSize / 2.
                     */
                    getLogger().info(r -> r.message("BE WRITABLE AGAIN, RESUME"));
                    socket.resume();
                    whenDrain();
                })
                .closeHandler(close -> {
                    getLogger().info(r -> r.message("SOCKET CLOSE"));
                    this.funnel.undeployMe();
                    whenClose();
                })
                .exceptionHandler(throwable -> {
                    getLogger().exception(throwable, r -> r.message("socket exception"));
                    whenExceptionOccurred(throwable);
                });
    }

    protected LoggerFactory getLoggerFactory() {
        return Keel.getLoggerFactory();
    }

    public final SpecificLogger<SocketSpecificLog> getLogger() {
        return logger;
    }

    private SpecificLogger<SocketSpecificLog> buildLogger() {
        return getLoggerFactory().createLogger(SocketSpecificLog.TopicTcpSocket, () -> new SocketSpecificLog().classification(List.of("socket_id:" + socketID)));
    }

    public String getSocketID() {
        return socketID;
    }

    abstract protected Future<Void> whenBufferComes(Buffer incomingBuffer);

    protected void whenReadToEnd() {

    }

    protected void whenDrain() {

    }

    protected void whenClose() {

    }

    protected void whenExceptionOccurred(Throwable throwable) {

    }

    public Future<Void> write(String s) {
        Future<Void> future = this.socket.write(s);
        if (this.socket.writeQueueFull()) {
            this.socket.pause();
            getLogger().info(r -> r.message("Write Queue Full, PAUSE"));
        }
        return future;
    }

    public Future<Void> write(String s, String enc) {
        Future<Void> future = this.socket.write(s, enc);
        if (this.socket.writeQueueFull()) {
            this.socket.pause();
            getLogger().info(r -> r.message("Write Queue Full, PAUSE"));
        }
        return future;
    }

    public Future<Void> write(Buffer buffer) {
        Future<Void> future = this.socket.write(buffer);
        if (this.socket.writeQueueFull()) {
            this.socket.pause();
            getLogger().info(r -> r.message("Write Queue Full, PAUSE"));
        }
        return future;
    }

    public SocketAddress getRemoteAddress() {
        return this.socket.remoteAddress();
    }

    public SocketAddress getLocalAddress() {
        return this.socket.localAddress();
    }

    public String getRemoteAddressString() {
        return this.socket.remoteAddress().host() + ":" + this.socket.remoteAddress().port();
    }

    public String getLocalAddressString() {
        return this.socket.localAddress().host() + ":" + this.socket.localAddress().port();
    }

    public Future<Void> close() {
        return this.socket.close();
    }

    public KeelAbstractSocketWrapper setMaxSize(int maxSize) {
        this.socket.setWriteQueueMaxSize(maxSize);
        return this;
    }
}
