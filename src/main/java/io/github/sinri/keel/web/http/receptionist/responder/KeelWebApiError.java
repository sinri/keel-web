package io.github.sinri.keel.web.http.receptionist.responder;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 处理网络接口请求时发生错误
 *
 * @since 5.0.0
 */
public class KeelWebApiError extends RuntimeException {
    private final int statusCode;

    public KeelWebApiError(@NotNull String message) {
        this(200, message, null);
    }

    public KeelWebApiError(@NotNull String message, @Nullable Throwable cause) {
        this(200, message, cause);
    }

    public KeelWebApiError(int statusCode, @NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public static KeelWebApiError wrap(@NotNull Throwable throwable) {
        return new KeelWebApiError(200, "Web API Error with message: " + throwable.getMessage(), throwable);
    }

    public int getStatusCode() {
        return statusCode;
    }
}
