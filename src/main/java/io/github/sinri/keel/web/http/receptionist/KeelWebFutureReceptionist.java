package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.base.Keel;
import io.github.sinri.keel.web.http.receptionist.responder.KeelWebApiError;
import io.github.sinri.keel.web.http.receptionist.responder.KeelWebResponder;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;

/**
 * 指定使用异步逻辑构成主逻辑的请求接待类。
 *
 * @param <R> 返回内容的承载类型
 * @see KeelWebResponder
 * @since 5.0.0
 */
abstract public class KeelWebFutureReceptionist<R> extends KeelWebReceptionist {
    private final @NotNull KeelWebResponder<R> responder;

    public KeelWebFutureReceptionist(@NotNull Keel keel, @NotNull RoutingContext routingContext) {
        super(keel, routingContext);
        this.responder = buildResponder();
    }

    /**
     *
     * @return an instance of {@link KeelWebResponder}.
     */
    @NotNull
    abstract protected KeelWebResponder<R> buildResponder();

    @NotNull
    public final KeelWebResponder<R> getResponder() {
        return responder;
    }

    @Override
    public void handle() {
        getLogger().info(log -> log.message("TO HANDLE REQUEST"));

        Future.succeededFuture()
              .compose(v -> handleForFuture())
              .andThen(ar -> {
                  try {
                      if (ar.failed()) {
                          var throwable = ar.cause();
                          if (throwable instanceof KeelWebApiError) {
                              getResponder().respondOnFailure((KeelWebApiError) throwable);
                          } else {
                              getResponder().respondOnFailure(KeelWebApiError.wrap(throwable));
                          }
                      } else {
                          this.getResponder().respondOnSuccess(ar.result());
                      }
                  } catch (Throwable throwable) {
                      getLogger().error(event -> event
                              .message("RoutingContext has been dealt by others")
                              .setRespondInfo(
                                      getRoutingContext().response().getStatusCode(),
                                      getRoutingContext().response().getStatusMessage(),
                                      getRoutingContext().response().ended(),
                                      getRoutingContext().response().closed()
                              )
                              .exception(throwable));
                  }
              });
    }

    @NotNull
    abstract protected Future<R> handleForFuture();
}
