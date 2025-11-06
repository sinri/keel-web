package io.github.sinri.keel.web.http.requester.extractor;

import io.github.sinri.keel.web.http.requester.error.ReceivedAbnormalStatusResponse;
import io.github.sinri.keel.web.http.requester.error.ReceivedUnexpectedFormatResponse;
import io.github.sinri.keel.web.http.requester.error.ReceivedUnexpectedResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * A dynamic extractor designed to process HTTP responses by validating desired status codes
 * and applying a transformation function to the response body.
 * This class is primarily for scenarios where the response body format may vary depending on the use case.
 *
 * @param <T> The data type of the extracted result after applying the transformer function.
 * @since 4.1.5
 */
public class KeelWebResponseDynamicExtractor<T> {
    private final @Nullable String defaultRequestLabel;
    private final @Nullable Set<Integer> expectedStatusCodes;
    private final @Nonnull Function<Buffer, T> transformer;

    /**
     * Constructs an instance of {@code KeelWebResponseDynamicExtractor} for processing HTTP responses
     * with customizable validation of status codes and transformation of the response body.
     *
     * @param defaultRequestLabel An optional label for the request, used for logging or debugging.
     *                            If null, a default label will be applied during extraction.
     * @param transformer         A non-null function to transform the response body {@link Buffer}
     *                            into the desired output format.
     * @param expectedStatusCodes An optional set of HTTP status codes that are considered valid.
     *                            If null or not specified, no specific status codes will be validated.
     */
    public KeelWebResponseDynamicExtractor(
            @Nullable String defaultRequestLabel,
            @Nonnull Function<Buffer, T> transformer,
            @Nullable Set<Integer> expectedStatusCodes
    ) {
        this.defaultRequestLabel = defaultRequestLabel;
        this.expectedStatusCodes = expectedStatusCodes;
        this.transformer = transformer;
    }

    /**
     * Constructs an instance of {@code KeelWebResponseDynamicExtractor} for processing HTTP responses
     * with customizable transformation of the response body. The expected status codes default to {@code 200}.
     *
     * @param defaultRequestLabel An optional label for the request, used for logging or debugging.
     *                            If null, a default label will be applied during extraction.
     * @param transformer         A non-null function to transform the response body {@link Buffer}
     *                            into the desired output format.
     */
    public KeelWebResponseDynamicExtractor(
            @Nullable String defaultRequestLabel,
            @Nonnull Function<Buffer, T> transformer) {
        this(defaultRequestLabel, transformer, Set.of(200));
    }

    /**
     * Constructs an instance of {@code KeelWebResponseDynamicExtractor} for processing HTTP responses
     * with customizable transformation of the response body. The expected status codes default to {@code 200}.
     *
     * @param transformer A non-null function to transform the response body {@link Buffer}
     *                    into the desired output format.
     */
    public KeelWebResponseDynamicExtractor(@Nonnull Function<Buffer, T> transformer) {
        this(null, transformer, Set.of(200));
    }

    /**
     * Extracts a transformed result from the given HTTP response. If the response does not meet the expected
     * criteria, an exception is thrown.
     *
     * @param response the HTTP response to be processed; must not be null
     * @return the transformed result obtained from the response body
     * @throws ReceivedUnexpectedResponse if the response status code or format does not meet the expected criteria
     */
    public T extract(@Nonnull HttpResponse<Buffer> response) throws ReceivedUnexpectedResponse {
        String requestLabel = Objects.requireNonNullElse(defaultRequestLabel, "Unlabelled");
        return this.extract(requestLabel, response);
    }

    /**
     * Extracts a transformed result from the given HTTP response, validating the response status code and body format.
     * If the response does not meet the expected criteria, an exception is thrown.
     *
     * @param requestLabel the label identifying the request, used for logging or debugging; must not be null
     * @param response     the HTTP response to be processed; must not be null
     * @return the transformed result obtained from the response body
     * @throws ReceivedUnexpectedResponse if the response status code is not as expected or the response body format is
     *                                    invalid
     */
    public T extract(@Nonnull String requestLabel, @Nonnull HttpResponse<Buffer> response) throws ReceivedUnexpectedResponse {
        var responseStatusCode = response.statusCode();
        Buffer responseBody = response.body();
        if (expectedStatusCodes != null && !expectedStatusCodes.contains(responseStatusCode)) {
            throw new ReceivedAbnormalStatusResponse(requestLabel, responseStatusCode, responseBody);
        }
        try {
            return transformer.apply(responseBody);
        } catch (Throwable e) {
            throw new ReceivedUnexpectedFormatResponse(requestLabel, responseStatusCode, responseBody);
        }
    }
}
