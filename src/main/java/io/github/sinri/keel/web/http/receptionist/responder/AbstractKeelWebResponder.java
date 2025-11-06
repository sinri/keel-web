package io.github.sinri.keel.web.http.receptionist.responder;

import io.github.sinri.keel.core.ValueBox;
import io.github.sinri.keel.logger.KeelLogLevel;
import io.github.sinri.keel.logger.issue.recorder.KeelIssueRecorder;
import io.github.sinri.keel.web.http.prehandler.KeelPlatformHandler;
import io.github.sinri.keel.web.http.receptionist.ReceptionistIssueRecord;
import io.vertx.ext.web.RoutingContext;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

import static io.github.sinri.keel.web.http.receptionist.KeelWebReceptionist.parseWebClientIPChain;

/**
 * @since 4.0.6
 */
public abstract class AbstractKeelWebResponder implements KeelWebResponder {
    private final @Nonnull RoutingContext routingContext;
    private final @Nonnull KeelIssueRecorder<ReceptionistIssueRecord> issueRecorder;

    /**
     * Constructs an AbstractKeelWebResponder instance with the given routing context and issue recorder.
     *
     * @param routingContext the routing context associated with the request, must not be null
     * @param issueRecorder  the recorder for tracking and recording issues during the processing of the request, must
     *                       not be null
     */
    public AbstractKeelWebResponder(@Nonnull RoutingContext routingContext, @Nonnull KeelIssueRecorder<ReceptionistIssueRecord> issueRecorder) {
        this.routingContext = routingContext;
        this.issueRecorder = issueRecorder;
    }

    /**
     * Retrieves the associated {@link RoutingContext}.
     *
     * @return the {@link RoutingContext} instance associated with this responder, never null
     */
    @Nonnull
    public RoutingContext getRoutingContext() {
        return routingContext;
    }

    /**
     * Retrieves the associated issue recorder for managing and recording issues specific to
     * {@link ReceptionistIssueRecord}.
     *
     * @return an instance of {@link KeelIssueRecorder} configured for handling {@link ReceptionistIssueRecord},
     *         never null
     */
    @Nonnull
    public KeelIssueRecorder<ReceptionistIssueRecord> getIssueRecorder() {
        return issueRecorder;
    }

    /**
     * Determines whether verbose logging is enabled by comparing the current visible log level
     * with the debug log level threshold.
     *
     * @return {@code true} if the current visible log level is at least as severe as the DEBUG level;
     *         {@code false} otherwise.
     */
    @Override
    public boolean isVerboseLogging() {
        KeelLogLevel visibleLevel = getIssueRecorder().getVisibleLevel();
        return KeelLogLevel.DEBUG.isEnoughSeriousAs(visibleLevel);
    }

    /**
     * Records the given response object in the issue recorder if verbose logging is enabled.
     *
     * @param response the response object to record
     */
    protected void recordResponseVerbosely(Object response) {
        if (isVerboseLogging()) {
            getIssueRecorder().debug(r -> r.setResponse(response));
        }
    }

    /**
     * @return the request id.
     * @since 3.0.8 mark it nullable as it might be null.
     */
    public @Nonnull String readRequestID() {
        return Objects.requireNonNull(routingContext.get(KeelPlatformHandler.KEEL_REQUEST_ID));
    }

    /**
     * @return the request start time.
     * @since 3.0.8 mark it nullable as it might be null.
     */
    public @Nonnull Long readRequestStartTime() {
        return Objects.requireNonNull(routingContext.get(KeelPlatformHandler.KEEL_REQUEST_START_TIME));
    }

    public @Nonnull List<String> readRequestIPChain() {
        return parseWebClientIPChain(routingContext);
    }

    /**
     * @deprecated let this deprecated method be final.
     */
    @Deprecated(since = "4.1.0")
    public final void respondOnFailure(@Nonnull Throwable throwable, @Nonnull ValueBox<?> dataValueBox) {
        KeelWebResponder.super.respondOnFailure(throwable, dataValueBox);
    }

    /**
     * @deprecated let this deprecated method be final.
     */
    @Deprecated(since = "4.1.0")
    public final void respondOnFailure(@Nonnull Throwable throwable) {
        respondOnFailure(throwable, new ValueBox<>());
    }
}
