package io.github.sinri.keel.web.tcp;

import io.github.sinri.keel.base.Keel;
import io.github.sinri.keel.core.servant.funnel.Funnel;
import io.github.sinri.keel.logger.api.factory.LoggerFactory;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;


/**
 * 针对{@link NetSocket}的封装类。
 *
 * @since 5.0.0
 */
abstract public class KeelAbstractSocketWrapper {
    @NotNull
    private final String socketID;
    @NotNull
    private final NetSocket socket;
    @NotNull
    private final Keel keel;
    @NotNull
    private final Funnel funnel;
    @NotNull
    private final SpecificLogger<SocketSpecificLog> logger;

    public KeelAbstractSocketWrapper(@NotNull Keel keel, @NotNull NetSocket socket) {
        this(keel, socket, UUID.randomUUID().toString());
    }

    public KeelAbstractSocketWrapper(@NotNull Keel keel, @NotNull NetSocket socket, @NotNull String socketID) {
        this.keel = keel;
        this.socketID = socketID;
        this.socket = socket;

        this.logger = this.buildLogger();

        this.funnel = new Funnel(keel);
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
                    getLogger().error(r -> r.message("socket exception").exception(throwable));
                    whenExceptionOccurred(throwable);
                });
    }

    @NotNull
    protected LoggerFactory getLoggerFactory() {
        return keel.getLoggerFactory();
    }

    public final @NotNull SpecificLogger<SocketSpecificLog> getLogger() {
        return logger;
    }

    @NotNull
    private SpecificLogger<SocketSpecificLog> buildLogger() {
        return getLoggerFactory().createLogger(SocketSpecificLog.TopicTcpSocket, () -> new SocketSpecificLog().classification(List.of("socket_id:" + socketID)));
    }

    public @NotNull String getSocketID() {
        return socketID;
    }

    @NotNull
    abstract protected Future<Void> whenBufferComes(Buffer incomingBuffer);

    protected void whenReadToEnd() {

    }

    protected void whenDrain() {

    }

    protected void whenClose() {

    }

    protected void whenExceptionOccurred(@NotNull Throwable throwable) {

    }

    @NotNull
    public Future<Void> write(@NotNull String s) {
        Future<Void> future = this.socket.write(s);
        if (this.socket.writeQueueFull()) {
            this.socket.pause();
            getLogger().info(r -> r.message("Write Queue Full, PAUSE"));
        }
        return future;
    }

    @NotNull
    public Future<Void> write(@NotNull String s, @NotNull String enc) {
        Future<Void> future = this.socket.write(s, enc);
        if (this.socket.writeQueueFull()) {
            this.socket.pause();
            getLogger().info(r -> r.message("Write Queue Full, PAUSE"));
        }
        return future;
    }

    @NotNull
    public Future<Void> write(@NotNull Buffer buffer) {
        Future<Void> future = this.socket.write(buffer);
        if (this.socket.writeQueueFull()) {
            this.socket.pause();
            getLogger().info(r -> r.message("Write Queue Full, PAUSE"));
        }
        return future;
    }

    @Nullable
    public SocketAddress getRemoteAddress() {
        return this.socket.remoteAddress();
    }

    @Nullable
    public SocketAddress getLocalAddress() {
        return this.socket.localAddress();
    }

    @NotNull
    public String getRemoteAddressString() {
        return this.socket.remoteAddress().host() + ":" + this.socket.remoteAddress().port();
    }

    @NotNull
    public String getLocalAddressString() {
        return this.socket.localAddress().host() + ":" + this.socket.localAddress().port();
    }

    @NotNull
    public Future<Void> close() {
        return this.socket.close();
    }

    @NotNull
    public KeelAbstractSocketWrapper setMaxSize(int maxSize) {
        this.socket.setWriteQueueMaxSize(maxSize);
        return this;
    }
}
