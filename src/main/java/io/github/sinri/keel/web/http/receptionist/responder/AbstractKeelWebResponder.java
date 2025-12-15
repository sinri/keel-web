package io.github.sinri.keel.web.http.receptionist.responder;

import io.github.sinri.keel.logger.api.LogLevel;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.github.sinri.keel.web.http.prehandler.KeelPlatformHandler;
import io.github.sinri.keel.web.http.receptionist.ReceptionistSpecificLog;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static io.github.sinri.keel.web.http.receptionist.KeelWebReceptionist.parseWebClientIPChain;

/**
 * Web 请求响应器的基础实现类。
 *
 * @since 5.0.0
 */
public abstract class AbstractKeelWebResponder<R> implements KeelWebResponder<R> {
    private final @NotNull RoutingContext routingContext;
    private final @NotNull SpecificLogger<ReceptionistSpecificLog> issueRecorder;

    /**
     * Constructs an AbstractKeelWebResponder instance with the given routing context and issue recorder.
     *
     * @param routingContext the routing context associated with the request, must not be null
     * @param issueRecorder  the recorder for tracking and recording issues during the processing of the request, must
     *                       not be null
     */
    public AbstractKeelWebResponder(@NotNull RoutingContext routingContext, @NotNull SpecificLogger<ReceptionistSpecificLog> issueRecorder) {
        this.routingContext = routingContext;
        this.issueRecorder = issueRecorder;
    }

    /**
     * Retrieves the associated {@link RoutingContext}.
     *
     * @return the {@link RoutingContext} instance associated with this responder, never null
     */
    @NotNull
    public RoutingContext getRoutingContext() {
        return routingContext;
    }

    /**
     * Retrieves the associated issue recorder for managing and recording issues specific to
     * {@link ReceptionistSpecificLog}.
     *
     * @return an instance of {@link SpecificLogger} configured for handling {@link ReceptionistSpecificLog},
     *         never null
     */
    @NotNull
    public SpecificLogger<ReceptionistSpecificLog> getIssueRecorder() {
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
        LogLevel visibleLevel = getIssueRecorder().visibleLevel();
        return LogLevel.DEBUG.isEnoughSeriousAs(visibleLevel);
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
     *
     */
    public @NotNull String readRequestID() {
        return Objects.requireNonNull(routingContext.get(KeelPlatformHandler.KEEL_REQUEST_ID));
    }

    /**
     * @return the request start time.
     */
    public @NotNull Long readRequestStartTime() {
        return Objects.requireNonNull(routingContext.get(KeelPlatformHandler.KEEL_REQUEST_START_TIME));
    }

    public @NotNull List<String> readRequestIPChain() {
        return parseWebClientIPChain(routingContext);
    }
}
