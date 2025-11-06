package io.github.sinri.keel.web.http.prehandler;

import io.github.sinri.keel.web.http.receptionist.ApiMeta;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The class provided a basic implementation.
 * You may override it to support more requirements.
 *
 * @since 3.2.13
 */
public class PreHandlerChain {
    protected final List<PlatformHandler> platformHandlers = new ArrayList<>();

    protected final List<SecurityPolicyHandler> securityPolicyHandlers = new ArrayList<>();
    protected final List<ProtocolUpgradeHandler> protocolUpgradeHandlers = new ArrayList<>();
    protected final List<MultiTenantHandler> multiTenantHandlers = new ArrayList<>();
    /**
     * Tells who the user is
     */
    protected final List<AuthenticationHandler> authenticationHandlers = new ArrayList<>();
    protected final List<InputTrustHandler> inputTrustHandlers = new ArrayList<>();
    /**
     * Tells what the user is allowed to do
     */
    protected final List<AuthorizationHandler> authorizationHandlers = new ArrayList<>();
    protected final List<Handler<RoutingContext>> userHandlers = new ArrayList<>();

    protected String uploadDirectory = BodyHandler.DEFAULT_UPLOADS_DIRECTORY;

    protected Handler<RoutingContext> failureHandler = null;

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
