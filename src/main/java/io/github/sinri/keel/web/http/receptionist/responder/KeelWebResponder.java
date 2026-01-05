package io.github.sinri.keel.web.http.receptionist.responder;

import io.github.sinri.keel.core.utils.value.ValueBox;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.github.sinri.keel.web.http.receptionist.ReceptionistSpecificLog;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


/**
 * Web 请求响应器。
 * <p>
 * 对于失败请求，仅接受{@link KeelWebApiError}兼容的异常。
 *
 * @param <R> 返回内容的承载类型
 * @since 5.0.0
 */
@NullMarked
public interface KeelWebResponder<R> {
    static KeelWebResponder<JsonObject> createCommonInstance(RoutingContext routingContext, SpecificLogger<ReceptionistSpecificLog> issueRecorder) {
        return new KeelWebResponderCommonApiImpl(routingContext, issueRecorder);
    }

    void respondOnSuccess(@Nullable R data);

    void respondOnFailure(KeelWebApiError webApiError, @Nullable ValueBox<?> dataValueBox);

    /**
     * @param throwable the thrown {@link KeelWebApiError} instance
     */
    default void respondOnFailure(KeelWebApiError throwable) {
        respondOnFailure(throwable, null);
    }

    boolean isVerboseLogging();

    /**
     *
     * @return 正常应答时，返回头 Content-Type 的内容，应匹配返回内容的承载类型
     */
    String contentTypeToRespond();
}
