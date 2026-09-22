package io.github.sinri.keel.web.http.receptionist.responder;


import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 处理网络接口请求时发生错误。
 * <p>
 * 显式构造的异常消息会公开给客户端，调用方应确保消息适合公开；cause 仅用于服务端诊断。
 *
 * @since 5.0.0
 */
@NullMarked
public class KeelWebApiError extends RuntimeException {
    private final int statusCode;

    public KeelWebApiError(String message) {
        this(200, message, null);
    }

    public KeelWebApiError(String message, @Nullable Throwable cause) {
        this(200, message, cause);
    }

    public KeelWebApiError(int statusCode, String message, @Nullable Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * 将一个非KeelWebApiError的异常包装为KeelWebApiError并以 500 返回。
     * 使用固定的公开消息，原始异常保留在 cause 中。
     *
     * @param throwable WEB 处理过程中的异常
     * @return 包装好的 KeelWebApiError 实例
     */
    public static KeelWebApiError wrap(Throwable throwable) {
        if (throwable instanceof KeelWebApiError apiError) {
            return apiError;
        }

        return new KeelWebApiError(
                500,
                "Internal server error",
                throwable
        );
    }

    public int getStatusCode() {
        return statusCode;
    }
}
