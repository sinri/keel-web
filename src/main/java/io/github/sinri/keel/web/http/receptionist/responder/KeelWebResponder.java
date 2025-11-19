package io.github.sinri.keel.web.http.receptionist.responder;

import io.github.sinri.keel.core.utils.value.ValueBox;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.github.sinri.keel.web.http.receptionist.ReceptionistIssueRecord;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * @since 4.0.4
 */
public interface KeelWebResponder {
    static KeelWebResponder createCommonInstance(@NotNull RoutingContext routingContext, @NotNull SpecificLogger<ReceptionistIssueRecord> issueRecorder) {
        return new KeelWebResponderCommonApiImpl(routingContext, issueRecorder);
    }

    void respondOnSuccess(@Nullable Object data);

    /**
     * @deprecated As of 4.1.0, this method should be replaced by
     *         {@link KeelWebResponder#respondOnFailure(KeelWebApiError, ValueBox)}.
     */
    @Deprecated(since = "4.1.0")
    default void respondOnFailure(@NotNull Throwable throwable, @NotNull ValueBox<?> dataValueBox) {
        if (throwable instanceof KeelWebApiError) {
            respondOnFailure((KeelWebApiError) throwable, dataValueBox);
        } else {
            respondOnFailure(KeelWebApiError.wrap(throwable), dataValueBox);
        }
    }

    /**
     * @since 4.1.0
     */
    void respondOnFailure(@NotNull KeelWebApiError webApiError, @NotNull ValueBox<?> dataValueBox);

    /**
     * @deprecated As of 4.1.0, this method should be replaced by
     *         {@link KeelWebResponder#respondOnFailure(KeelWebApiError)}.
     */
    @Deprecated(since = "4.1.0")
    default void respondOnFailure(@NotNull Throwable throwable) {
        respondOnFailure(throwable, new ValueBox<>());
    }

    /**
     * @param throwable the thrown {@link KeelWebApiError} instance
     * @since 4.1.0
     */
    default void respondOnFailure(@NotNull KeelWebApiError throwable) {
        respondOnFailure(throwable, new ValueBox<>());
    }

    boolean isVerboseLogging();
}
