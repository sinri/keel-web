package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.base.Keel;
import io.github.sinri.keel.web.http.receptionist.responder.KeelWebApiError;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;

/**
 * 指定使用异步逻辑构成主逻辑的请求接待类，专用于虚拟线程环境下。
 *
 * @param <R> 返回内容的承载类型
 * @since 5.0.0
 */
public abstract class KeelWebFutureReceptionistInVirtualThread<R> extends KeelWebFutureReceptionist<R> {
    public KeelWebFutureReceptionistInVirtualThread(@NotNull Keel keel, @NotNull RoutingContext routingContext) {
        super(keel, routingContext);
    }

    /**
     * 本方法在虚拟线程中运行。
     * 如运行同步逻辑，必须注意不阻塞；如使用异步逻辑则返回 await 后得到的结果。
     *
     * @return 接口返回内容，将通过{@link KeelWebFutureReceptionist#getResponder()}输出
     * @throws KeelWebApiError 接口运行过程中的报错，将通过{@link KeelWebFutureReceptionist#getResponder()}输出
     */
    @NotNull
    protected abstract R handleInVirtualThread() throws KeelWebApiError;

    @Override
    protected final @NotNull Future<R> handleForFuture() {
        try {
            var r = handleInVirtualThread();
            return Future.succeededFuture(r);
        } catch (KeelWebApiError e) {
            return Future.failedFuture(e);
        }
    }
}
