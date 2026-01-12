package io.github.sinri.keel.web.http.prehandler;

import io.github.sinri.keel.web.http.receptionist.ApiMeta;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 对一个请求进行预处理的预处理器链。
 * <p>
 * 本类为基本实现，可以在子类中实现更多自定义内容。
 * 通常是在子类的构造方法中对各种类型的处理器列表进行必要的新增。
 *
 * @since 5.0.0
 */
@NullMarked
public class PreHandlerChain {
    /**
     * @see KeelPlatformHandler
     */
    protected final List<PlatformHandler> platformHandlers = new ArrayList<>();
    protected final List<SecurityPolicyHandler> securityPolicyHandlers = new ArrayList<>();
    protected final List<ProtocolUpgradeHandler> protocolUpgradeHandlers = new ArrayList<>();
    protected final List<MultiTenantHandler> multiTenantHandlers = new ArrayList<>();
    /**
     * Tells who the user is.
     *
     * @see SimpleAuthenticationHandler
     * @see AuthenticationDelegate
     */
    protected final List<AuthenticationHandler> authenticationHandlers = new ArrayList<>();
    protected final List<InputTrustHandler> inputTrustHandlers = new ArrayList<>();
    /**
     * Tells what the user is allowed to do
     */
    protected final List<AuthorizationHandler> authorizationHandlers = new ArrayList<>();
    protected final List<Handler<RoutingContext>> userHandlers = new ArrayList<>();
    protected String uploadDirectory = BodyHandler.DEFAULT_UPLOADS_DIRECTORY;
    protected @Nullable Handler<RoutingContext> failureHandler = null;

    public PreHandlerChain() {
    }

    /**
     * 使用给定的{@link AuthenticationDelegate}实例，基于{@link SimpleAuthenticationHandler}提供的实现，创建一个{@link
     * AuthenticationHandler}实例。
     *
     * @param delegate 给定的{@link AuthenticationDelegate}实例，提供认证逻辑，解析请求上下文并异步确定对应授权访问者身份
     * @return {@link AuthenticationHandler}实例
     */
    protected static AuthenticationHandler buildAuthenticationHandlerWithDelegate(AuthenticationDelegate delegate) {
        return SimpleAuthenticationHandler.create().authenticate(delegate::authenticate);
    }

    public final void executeHandlers(Route route, ApiMeta apiMeta) {
        // === HANDLERS WEIGHT IN ORDER ===
        // PLATFORM
        route.handler(new KeelPlatformHandler());
        if (apiMeta.timeout() > 0) {
            // PlatformHandler
            route.handler(TimeoutHandler.create(apiMeta.timeout(), apiMeta.statusCodeForTimeout()));
        }
        route.handler(ResponseTimeHandler.create());
        this.platformHandlers.forEach(route::handler);

        //    SECURITY_POLICY,
        // SecurityPolicyHandler
        // CorsHandler: Cross Origin Resource Sharing
        this.securityPolicyHandlers.forEach(route::handler);

        //    PROTOCOL_UPGRADE,
        protocolUpgradeHandlers.forEach(route::handler);
        //    BODY,
        if (apiMeta.requestBodyNeeded()) {
            route.handler(BodyHandler.create(uploadDirectory));
        }
        //    MULTI_TENANT,
        multiTenantHandlers.forEach(route::handler);
        //    AUTHENTICATION,
        authenticationHandlers.forEach(route::handler);
        //    INPUT_TRUST,
        inputTrustHandlers.forEach(route::handler);
        //    AUTHORIZATION,
        authorizationHandlers.forEach(route::handler);
        //    USER
        userHandlers.forEach(route::handler);

        // failure handler
        if (failureHandler != null) {
            route.failureHandler(failureHandler);
        }
    }
}
