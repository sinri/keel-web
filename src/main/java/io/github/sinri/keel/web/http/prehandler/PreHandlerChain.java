package io.github.sinri.keel.web.http.prehandler;

import io.github.sinri.keel.web.http.receptionist.ApiMeta;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class PreHandlerChain {
    /**
     * @see KeelPlatformHandler
     */
    @NotNull
    protected final List<PlatformHandler> platformHandlers = new ArrayList<>();
    @NotNull
    protected final List<SecurityPolicyHandler> securityPolicyHandlers = new ArrayList<>();
    @NotNull
    protected final List<ProtocolUpgradeHandler> protocolUpgradeHandlers = new ArrayList<>();
    @NotNull
    protected final List<MultiTenantHandler> multiTenantHandlers = new ArrayList<>();
    /**
     * Tells who the user is.
     * @see SimpleAuthenticationHandler
     */
    @NotNull
    protected final List<AuthenticationHandler> authenticationHandlers = new ArrayList<>();
    @NotNull
    protected final List<InputTrustHandler> inputTrustHandlers = new ArrayList<>();
    /**
     * Tells what the user is allowed to do
     */
    @NotNull
    protected final List<AuthorizationHandler> authorizationHandlers = new ArrayList<>();
    @NotNull
    protected final List<Handler<RoutingContext>> userHandlers = new ArrayList<>();

    @NotNull
    protected String uploadDirectory = BodyHandler.DEFAULT_UPLOADS_DIRECTORY;

    @Nullable
    protected Handler<RoutingContext> failureHandler = null;

    public final void executeHandlers(@NotNull Route route, @NotNull ApiMeta apiMeta) {
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
