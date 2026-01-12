package io.github.sinri.keel.web.http.prehandler;

import io.vertx.core.Future;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.SimpleAuthenticationHandler;
import org.jspecify.annotations.NullMarked;

import java.util.function.Function;

/**
 * 用于{@link SimpleAuthenticationHandler}构建的代理定义。
 *
 * @since 5.0.0
 */
@NullMarked
public interface AuthenticationDelegate {
    static AuthenticationHandler build(Function<RoutingContext, Future<User>> authenticationFunction) {
        return SimpleAuthenticationHandler.create()
                                          .authenticate(authenticationFunction);
    }

    static AuthenticationHandler build(AuthenticationDelegate authenticationDelegate) {
        return SimpleAuthenticationHandler.create()
                                          .authenticate(authenticationDelegate::authenticate);
    }

    Future<User> authenticate(RoutingContext routingContext);
}
