package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.web.http.receptionist.responder.KeelWebApiError;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

/**
 * 指定使用异步逻辑构成主逻辑的请求接待类。
 *
 * @since 5.0.0
 */
abstract public class KeelWebFutureReceptionist extends KeelWebReceptionist {

    public KeelWebFutureReceptionist(RoutingContext routingContext) {
        super(routingContext);
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
                      getLogger().exception(throwable,
                              event -> event
                                      .message("RoutingContext has been dealt by others")
                                      .setRespondInfo(
                                              getRoutingContext().response().getStatusCode(),
                                              getRoutingContext().response().getStatusMessage(),
                                              getRoutingContext().response().ended(),
                                              getRoutingContext().response().closed()
                                      )
                      );
                  }
              });
    }

    abstract protected Future<Object> handleForFuture();
}
