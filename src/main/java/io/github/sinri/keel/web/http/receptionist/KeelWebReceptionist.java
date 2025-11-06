package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.logger.KeelLogLevel;
import io.github.sinri.keel.logger.issue.center.KeelIssueRecordCenter;
import io.github.sinri.keel.logger.issue.recorder.KeelIssueRecorder;
import io.github.sinri.keel.web.http.prehandler.KeelPlatformHandler;
import io.github.sinri.keel.web.http.receptionist.responder.KeelWebApiError;
import io.github.sinri.keel.web.http.receptionist.responder.KeelWebResponder;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @since 2.9.2
 * @since 3.0.0 TEST PASSED
 * @since 3.2.0 Moved the responding error for `dealt` logging logic out of the `respondOn*` methods.
 */
public abstract class KeelWebReceptionist {
    private final @Nonnull RoutingContext routingContext;
    private final @Nonnull KeelIssueRecorder<ReceptionistIssueRecord> issueRecorder;
    private final @Nonnull KeelWebResponder responder;

    public KeelWebReceptionist(@Nonnull RoutingContext routingContext) {
        this.routingContext = routingContext;
        this.issueRecorder = issueRecordCenter().generateIssueRecorder(ReceptionistIssueRecord.TopicReceptionist, () -> new ReceptionistIssueRecord(readRequestID()));
        if (isVerboseLogging()) {
            this.issueRecorder.setVisibleLevel(KeelLogLevel.DEBUG);
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

    @Nonnull
    public static List<String> parseWebClientIPChain(@Nonnull RoutingContext ctx) {
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

    @Nonnull
    protected final RoutingContext getRoutingContext() {
        return routingContext;
    }

    /**
     * @return (as of 4.0.4) whether to use DEBUG logging.
     */
    protected boolean isVerboseLogging() {
        return false;
    }

    /**
     * @return return a reference of an independent KeelIssueRecordCenter instance; do not create an instance here.
     * @since 3.2.0
     */
    @Nonnull
    abstract protected KeelIssueRecordCenter issueRecordCenter();

    /**
     * @since 4.0.4
     */
    protected KeelWebResponder buildResponder() {
        return KeelWebResponder.createCommonInstance(routingContext, issueRecorder);
    }

    /**
     * @return the built KeelWebResponder instance of this class instance.
     * @since 4.0.4
     */
    @Nonnull
    public final KeelWebResponder getResponder() {
        return responder;
    }

    /**
     * @since 3.2.0
     */
    @Nonnull
    public final KeelIssueRecorder<ReceptionistIssueRecord> getIssueRecorder() {
        return issueRecorder;
    }

    abstract public void handle();

    /**
     * As of 4.0.4, this method is not overrideable. Use {@link KeelWebReceptionist#getResponder()}.
     * <p>
     * As of 4.1.3, no usage in Keel, to remove.
     *
     * @since 3.0.12 add request_id to output json object
     */
    @Deprecated(since = "4.1.3", forRemoval = true)
    protected final void respondOnSuccess(@Nullable Object data) {
        getResponder().respondOnSuccess(data);
        getIssueRecorder().info(r -> r.message("SUCCESS, TO RESPOND."));
    }

    /**
     * As of 4.0.4, this method is not overrideable. Use {@link KeelWebReceptionist#getResponder()}.
     * <p>
     * As of 4.1.3, no usage in Keel, to remove.
     *
     * @since 3.0.12 add request_id to output json object
     *
     */
    @Deprecated(since = "4.1.3", forRemoval = true)
    protected final void respondOnFailure(@Nonnull Throwable throwable) {
        if (throwable instanceof KeelWebApiError) {
            getResponder().respondOnFailure((KeelWebApiError) throwable);
        } else {
            getResponder().respondOnFailure(KeelWebApiError.wrap(throwable));
        }
        getIssueRecorder().exception(throwable, r -> r.message("FAILED, TO RESPOND."));
    }

    /**
     * @since 3.0.8 mark it nullable as it might be null.
     */
    public @Nonnull String readRequestID() {
        return Objects.requireNonNull(routingContext.get(KeelPlatformHandler.KEEL_REQUEST_ID));
    }

    /**
     * @since 3.0.8 mark it nullable as it might be null.
     */
    public @Nonnull Long readRequestStartTime() {
        return Objects.requireNonNull(routingContext.get(KeelPlatformHandler.KEEL_REQUEST_START_TIME));
    }

    public @Nonnull List<String> readRequestIPChain() {
        return parseWebClientIPChain(routingContext);
    }

    public User readRequestUser() {
        return routingContext.user();
    }

    /**
     * @since 3.0.1
     */
    protected void addCookie(String name, String value, Long maxAge, boolean httpOnly) {
        CookieImpl cookie = new CookieImpl(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(httpOnly);
        getRoutingContext().response().addCookie(cookie);
    }

    /**
     * @since 3.0.1
     */
    protected void addCookie(@Nonnull String name, @Nonnull String value, Long maxAge) {
        addCookie(name, value, maxAge, false);
    }

    /**
     * @since 3.0.1
     */
    protected void removeCookie(@Nonnull String name) {
        getRoutingContext().response().removeCookie(name);
    }
}
