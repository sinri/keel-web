package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.logger.api.LogLevel;
import io.github.sinri.keel.logger.api.factory.LoggerFactory;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.github.sinri.keel.web.http.prehandler.KeelPlatformHandler;
import io.github.sinri.keel.web.http.receptionist.responder.KeelWebResponder;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 请求接待类。
 *
 * @since 5.0.0
 */
public abstract class KeelWebReceptionist {
    private final @NotNull RoutingContext routingContext;
    private final @NotNull SpecificLogger<ReceptionistSpecificLog> issueRecorder;
    private final @NotNull KeelWebResponder responder;

    public KeelWebReceptionist(@NotNull RoutingContext routingContext) {
        this.routingContext = routingContext;
        this.issueRecorder = getLoggerFactory().createLogger(ReceptionistSpecificLog.TopicReceptionist, () -> new ReceptionistSpecificLog(readRequestID()));
        if (isVerboseLogging()) {
            this.issueRecorder.visibleLevel(LogLevel.DEBUG);
        }
        this.issueRecorder.info(r -> r.setRequest(
                routingContext.request().method(),
                routingContext.request().path(),
                this.getClass(),
                (isVerboseLogging() ? routingContext.request().query() : null),
                (isVerboseLogging() ? routingContext.body().asString() : null)
        ));
        this.responder = buildResponder();
    }

    @NotNull
    public static List<String> parseWebClientIPChain(@NotNull RoutingContext ctx) {
        // X-Forwarded-For
        JsonArray clientIPChain = new JsonArray();
        String xForwardedFor = ctx.request().getHeader("X-Forwarded-For");
        if (xForwardedFor != null) {
            String[] split = xForwardedFor.split("[ ,]+");
            for (var item : split) {
                clientIPChain.add(item);
            }
        }
        clientIPChain.add(ctx.request().remoteAddress().hostAddress());

        List<String> list = new ArrayList<>();
        clientIPChain.forEach(item -> list.add(item.toString()));
        return list;
    }

    @NotNull
    protected final RoutingContext getRoutingContext() {
        return routingContext;
    }

    /**
     * @return 是否记录详细日志
     */
    protected boolean isVerboseLogging() {
        return false;
    }

    @NotNull
    abstract protected LoggerFactory getLoggerFactory();

    @NotNull
    protected KeelWebResponder buildResponder() {
        return KeelWebResponder.createCommonInstance(routingContext, issueRecorder);
    }

    @NotNull
    public final KeelWebResponder getResponder() {
        return responder;
    }

    @NotNull
    public final SpecificLogger<ReceptionistSpecificLog> getLogger() {
        return issueRecorder;
    }

    /**
     * 基于{@link KeelWebReceptionist#getRoutingContext()}，处理本类实例请求。
     * <p>
     * 本方法运行结束后，本类不应残留其他逻辑。
     */
    abstract public void handle();

    //    /**
    //     * As of 4.0.4, this method is not overrideable. Use {@link KeelWebReceptionist#getResponder()}.
    //     * <p>
    //     * As of 4.1.3, no usage in Keel, to remove.
    //     *
    //     * @since 3.0.12 add request_id to output json object
    //     */
    //    @Deprecated(since = "4.1.3", forRemoval = true)
    //    protected final void respondOnSuccess(@Nullable Object data) {
    //        getResponder().respondOnSuccess(data);
    //        getLogger().info(r -> r.message("SUCCESS, TO RESPOND."));
    //    }

    //    /**
    //     * As of 4.0.4, this method is not overrideable. Use {@link KeelWebReceptionist#getResponder()}.
    //     * <p>
    //     * As of 4.1.3, no usage in Keel, to remove.
    //     *
    //     * @since 3.0.12 add request_id to output json object
    //     *
    //     */
    //    @Deprecated(since = "4.1.3", forRemoval = true)
    //    protected final void respondOnFailure(@NotNull Throwable throwable) {
    //        if (throwable instanceof KeelWebApiError) {
    //            getResponder().respondOnFailure((KeelWebApiError) throwable);
    //        } else {
    //            getResponder().respondOnFailure(KeelWebApiError.wrap(throwable));
    //        }
    //        getLogger().exception(throwable, "FAILED, TO RESPOND.");
    //    }

    /**
     * @return 后端赋予请求的ID
     */
    public @NotNull String readRequestID() {
        return Objects.requireNonNull(routingContext.get(KeelPlatformHandler.KEEL_REQUEST_ID));
    }

    /**
     * @return 请求到达后端开始处理的时间
     */
    public @NotNull Long readRequestStartTime() {
        return Objects.requireNonNull(routingContext.get(KeelPlatformHandler.KEEL_REQUEST_START_TIME));
    }

    /**
     *
     * @return 调用方请求 IP 链
     */
    public @NotNull List<String> readRequestIPChain() {
        return parseWebClientIPChain(routingContext);
    }

    /**
     *
     * @return 获取请求上下文中登记的用户实体
     */
    public User readRequestUser() {
        return routingContext.user();
    }

    protected void addCookie(String name, String value, Long maxAge, boolean httpOnly) {
        CookieImpl cookie = new CookieImpl(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(httpOnly);
        getRoutingContext().response().addCookie(cookie);
    }

    protected void removeCookie(@NotNull String name) {
        getRoutingContext().response().removeCookie(name);
    }
}
