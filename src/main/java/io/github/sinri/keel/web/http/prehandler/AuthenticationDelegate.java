package io.github.sinri.keel.web.http.prehandler;

import io.vertx.core.Future;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SimpleAuthenticationHandler;
import org.jspecify.annotations.NullMarked;

/**
 * 用于{@link SimpleAuthenticationHandler}构建的代理定义。
 *
 * @since 5.0.0
 */
@NullMarked
public interface AuthenticationDelegate {
    Future<User> authenticate(RoutingContext routingContext);
}
