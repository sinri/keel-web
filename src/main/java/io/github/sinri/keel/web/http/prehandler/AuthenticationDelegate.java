package io.github.sinri.keel.web.http.prehandler;

import io.github.sinri.keel.base.KeelHolder;
import io.vertx.core.Future;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SimpleAuthenticationHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 用于{@link SimpleAuthenticationHandler}构建的代理定义。
 *
 * @since 5.0.0
 */
public interface AuthenticationDelegate extends KeelHolder {
    @NotNull
    Future<User> authenticate(@NotNull RoutingContext routingContext);
}
