package io.github.sinri.keel.web.http.prehandler;

import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.impl.UserContextInternal;

/**
 * 基于资格的验证处理器。
 *
 * @since 5.0.0
 */
public abstract class KeelAuthenticationHandlerWithCredentials implements AuthenticationHandler, AuthenticationProvider {
    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.request().pause();

        Credentials credentials = getCredentials(routingContext);

        this.authenticate(credentials)
            .onSuccess(user -> {
                ((UserContextInternal) routingContext.userContext()).setUser(user);
                routingContext.request().resume();
                routingContext.next();
            })
            .onFailure(throwable -> {
                routingContext.fail(throwable);
            });
    }

    abstract protected Credentials getCredentials(RoutingContext routingContext);
}
