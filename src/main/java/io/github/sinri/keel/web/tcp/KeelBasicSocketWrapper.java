package io.github.sinri.keel.web.tcp;

import io.github.sinri.keel.base.Keel;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 针对{@link NetSocket}的动态实现封装类。
 *
 * @since 5.0.0
 */
@NullMarked
public class KeelBasicSocketWrapper extends KeelAbstractSocketWrapper {
    private Function<Buffer, Future<Void>> incomingBufferProcessor = buffer -> Future.succeededFuture();
    private Handler<@Nullable Void> readToEndHandler = event -> {
    };
    private Handler<@Nullable Void> drainHandler = event -> {
    };
    private Handler<@Nullable Void> closeHandler = event -> {
    };
    private Consumer<Throwable> exceptionHandler = throwable -> {
    };

    public KeelBasicSocketWrapper(Keel keel, NetSocket socket) {
        super(keel, socket);
    }

    public KeelBasicSocketWrapper(Keel keel, NetSocket socket, String socketID) {
        super(keel, socket, socketID);
    }

    public KeelBasicSocketWrapper setCloseHandler(Handler<Void> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public KeelBasicSocketWrapper setDrainHandler(Handler<Void> drainHandler) {
        this.drainHandler = drainHandler;
        return this;
    }

    public KeelBasicSocketWrapper setExceptionHandler(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public KeelBasicSocketWrapper setIncomingBufferProcessor(Function<Buffer, Future<Void>> incomingBufferProcessor) {
        this.incomingBufferProcessor = incomingBufferProcessor;
        return this;
    }

    public KeelBasicSocketWrapper setReadToEndHandler(Handler<Void> readToEndHandler) {
        this.readToEndHandler = readToEndHandler;
        return this;
    }

    @Override
    protected Future<Void> whenBufferComes(Buffer incomingBuffer) {
        return incomingBufferProcessor.apply(incomingBuffer);
    }

    @Override
    protected void whenReadToEnd() {
        super.whenReadToEnd();
        readToEndHandler.handle(null);
    }

    @Override
    protected void whenDrain() {
        super.whenDrain();
        drainHandler.handle(null);
    }

    @Override
    protected void whenClose() {
        super.whenClose();
        closeHandler.handle(null);
    }

    @Override
    protected void whenExceptionOccurred(Throwable throwable) {
        super.whenExceptionOccurred(throwable);
        exceptionHandler.accept(throwable);
    }
}
