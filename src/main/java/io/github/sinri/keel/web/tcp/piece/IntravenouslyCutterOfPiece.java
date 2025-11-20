package io.github.sinri.keel.web.tcp.piece;

import io.github.sinri.keel.core.cutter.IntravenouslyCutter;
import io.github.sinri.keel.core.servant.intravenous.KeelIntravenous;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.buffer.Buffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.sinri.keel.base.KeelInstance.Keel;

public abstract class IntravenouslyCutterOfPiece<P extends BufferPiece> implements IntravenouslyCutter<P> {
    private final KeelIntravenous<P> intravenous;
    private final AtomicBoolean readStopRef = new AtomicBoolean(false);
    private final AtomicReference<Throwable> stopCause = new AtomicReference<>();
    private Buffer buffer;

    public IntravenouslyCutterOfPiece() {
        this.buffer = Buffer.buffer();
        this.intravenous = KeelIntravenous.instant(getSingleDropProcessor());
        intravenous.deployMe(new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER));
    }

    /**
     * @return 针对一个 Piece 的处理器。
     */
    @Override
    abstract public KeelIntravenous.SingleDropProcessor<P> getSingleDropProcessor();

    @Override
    public final void acceptFromStream(@NotNull Buffer incomingBuffer) {
        if (incomingBuffer.length() > 0) {
            buffer.appendBuffer(incomingBuffer);
            this.doParseOnce();
        }
    }

    private synchronized void doParseOnce() {
        List<P> pieces = this.parseFirstPiecesFromBuffer(buffer);
        if (!pieces.isEmpty()) {
            int sum = 0;
            for (var piece : pieces) {
                this.intravenous.add(piece);
                sum += piece.getLength();
            }
            buffer = buffer.slice(sum, buffer.length());
        }
    }

    @Override
    public final void stopHere(@Nullable Throwable throwable) {
        if (!readStopRef.get()) {
            doParseOnce();
            stopCause.set(throwable);
            readStopRef.set(true);
        }
    }

    @Override
    public final Future<Void> waitForAllHandled() {
        return Keel.asyncCallRepeatedly(repeatedlyCallTask -> {
                       if (!this.readStopRef.get()) {
                           return Keel.asyncSleep(200L);
                       }
                       if (!intravenous.isNoDropsLeft()) {
                           return Keel.asyncSleep(100L);
                       }
                       intravenous.shutdown();
                       if (!intravenous.isUndeployed()) {
                           return Keel.asyncSleep(100L);
                       }
                       repeatedlyCallTask.stop();
                       return Future.succeededFuture();
                   })
                   .compose(v -> {
                       Throwable throwable = stopCause.get();
                       if (throwable != null) {
                           return Future.failedFuture(throwable);
                       }
                       return Future.succeededFuture();
                   });
    }

    /**
     * 尝试从给定的当前 Buffer 对象中从头解析出尽可能多的可用的 Piece 对象。
     *
     * @param currentBuffer 当前 Buffer 对象
     * @return 从头解析到的 Piece 对象构成的列表。
     */
    @NotNull
    abstract protected List<P> parseFirstPiecesFromBuffer(@NotNull Buffer currentBuffer);
}
