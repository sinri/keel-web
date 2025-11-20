package io.github.sinri.keel.web.http.receptionist.responder;

import io.github.sinri.keel.core.utils.value.ValueBox;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.github.sinri.keel.web.http.receptionist.ReceptionistSpecificLog;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Web 请求响应器。
 * <p>
 * 对于失败请求，仅接受{@link KeelWebApiError}兼容的异常。
 *
 * @since 5.0.0
 */
public interface KeelWebResponder {
    static KeelWebResponder createCommonInstance(@NotNull RoutingContext routingContext, @NotNull SpecificLogger<ReceptionistSpecificLog> issueRecorder) {
        return new KeelWebResponderCommonApiImpl(routingContext, issueRecorder);
    }

    void respondOnSuccess(@Nullable Object data);

    /**
     * @since 4.1.0
     */
    void respondOnFailure(@NotNull KeelWebApiError webApiError, @Nullable ValueBox<?> dataValueBox);

    /**
     * @param throwable the thrown {@link KeelWebApiError} instance
     * @since 4.1.0
     */
    default void respondOnFailure(@NotNull KeelWebApiError throwable) {
        respondOnFailure(throwable, null);
    }

    boolean isVerboseLogging();
}
