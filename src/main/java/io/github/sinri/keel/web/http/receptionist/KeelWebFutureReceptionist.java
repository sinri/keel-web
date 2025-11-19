package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.web.http.receptionist.responder.KeelWebApiError;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

/**
 * @since 2.9.2
 * @since 2.9.2 add authorization with privileges
 * @since 3.0.0 TEST PASSED
 */
abstract public class KeelWebFutureReceptionist extends KeelWebReceptionist {

    public KeelWebFutureReceptionist(RoutingContext routingContext) {
        super(routingContext);
    }

    @Override
    public void handle() {
        // since 3.1.5 add a starting log
        getIssueRecorder().info(log -> log.message("TO HANDLE REQUEST"));

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
                      getIssueRecorder().exception(throwable,
                              "RoutingContext has been dealt by others",
                              event -> event
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
