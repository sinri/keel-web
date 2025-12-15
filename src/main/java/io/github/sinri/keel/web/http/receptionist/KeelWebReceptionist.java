package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.base.Keel;
import io.github.sinri.keel.base.KeelHolder;
import io.github.sinri.keel.logger.api.LogLevel;
import io.github.sinri.keel.logger.api.factory.LoggerFactory;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.github.sinri.keel.web.http.prehandler.KeelPlatformHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 请求接待类。
 *
 * @since 5.0.0
 */
public abstract class KeelWebReceptionist implements KeelHolder {
    private final @NotNull RoutingContext routingContext;
    private final @NotNull SpecificLogger<ReceptionistSpecificLog> logger;
    private final @NotNull Keel keel;

    public KeelWebReceptionist(@NotNull Keel keel, @NotNull RoutingContext routingContext) {
        this.keel = keel;
        this.routingContext = routingContext;
        this.logger = getLoggerFactory().createLogger(ReceptionistSpecificLog.TopicReceptionist, () -> new ReceptionistSpecificLog(readRequestID()));
        if (isVerboseLogging()) {
            this.logger.visibleLevel(LogLevel.DEBUG);
        }
        this.logger.info(r -> r.setRequest(
                routingContext.request().method(),
                routingContext.request().path(),
                this.getClass(),
                (isVerboseLogging() ? routingContext.request().query() : null),
                (isVerboseLogging() ? routingContext.body().asString() : null)
        ));
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

    public final @NotNull Keel getKeel() {
        return keel;
    }

    @NotNull
    protected final RoutingContext getRoutingContext() {
        return routingContext;
    }

    @NotNull
    public final Vertx getVertx() {
        return getRoutingContext().vertx();
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
    public final SpecificLogger<ReceptionistSpecificLog> getLogger() {
        return logger;
    }

    /**
     * 基于{@link KeelWebReceptionist#getRoutingContext()}，处理本类实例请求。
     * <p>
     * 本方法运行结束后，本类不应残留其他逻辑。
     */
    abstract public void handle();

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
    @Nullable
    public User readRequestUser() {
        return routingContext.user();
    }

    protected void addCookie(@NotNull String name, @NotNull String value, @Nullable String path, @Nullable Long maxAge, boolean httpOnly) {
        Cookie cookie1 = Cookie.cookie(name, value);
        cookie1.setPath(Objects.requireNonNullElse(path, "/"));
        if (maxAge != null) {
            cookie1.setMaxAge(maxAge);
        }
        cookie1.setHttpOnly(httpOnly);
        getRoutingContext().response().addCookie(cookie1);
    }

    protected void removeCookie(@NotNull String name) {
        getRoutingContext().response().removeCookie(name);
    }
}
