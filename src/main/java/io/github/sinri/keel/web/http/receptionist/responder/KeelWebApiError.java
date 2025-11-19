package io.github.sinri.keel.web.http.receptionist.responder;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Any exception thrown from receptionists to responder should be and recommended to be instance-of this class;
 * or would be automatically wrapped to this class.
 * <p>
 * As of 4.1.1, this class public its constructors and wrap method.
 *
 * @since 4.1.0
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

    /**
     * Throws a new {@link KeelWebApiError} with the specified status code, message, and cause.
     *
     * @param statusCode the HTTP status code associated with the error
     * @param message    the detail message for the error (must not be null)
     * @param throwable  the cause of the error (must not be null)
     */
    public static void issue(int statusCode, @NotNull String message, @Nullable Throwable throwable) throws KeelWebApiError {
        throw new KeelWebApiError(statusCode, message, throwable);
    }

    /**
     * Throws a new {@link KeelWebApiError} with a default status code of 200, the specified message, and no cause.
     *
     * @param message the detail message for the error (must not be null)
     */
    public static void issue(@NotNull String message) throws KeelWebApiError {
        issue(200, message, null);
    }

    /**
     * Throws a new {@link KeelWebApiError} with a default status code of 200, a message constructed from the given
     * throwable, and the throwable itself as the cause.
     *
     * @param throwable the cause of the error (must not be null)
     */
    public static void issue(@NotNull Throwable throwable) throws KeelWebApiError {
        throw wrap(throwable);
    }

    public static KeelWebApiError wrap(@NotNull Throwable throwable) {
        return new KeelWebApiError(200, "Web API Error with message: " + throwable.getMessage(), throwable);
    }

    public int getStatusCode() {
        return statusCode;
    }
}
