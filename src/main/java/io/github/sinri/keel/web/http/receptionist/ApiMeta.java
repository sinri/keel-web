package io.github.sinri.keel.web.http.receptionist;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @since 2.0
 * @since 3.2.12 it is repeatable now!
 */
@Repeatable(ApiMetaContainer.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMeta {
    String virtualHost() default "";

    String routePath();

    String[] allowMethods() default {"POST"};

    boolean requestBodyNeeded() default true;

    /**
     * @return timeout in ms. default is 10s. if 0, no timeout.
     * @since 2.9
     */
    long timeout() default 10_000;

    /**
     * @return the HTTP RESPONSE STATUS CODE for timeout.
     * @since 2.9
     */
    int statusCodeForTimeout() default 509;

    /**
     * @return It this path deprecated.
     * @since 3.2.11
     */
    boolean isDeprecated() default false;

    /**
     * @since 3.2.11
     */
    String remark() default "";

    //    /**
    //     * @since 3.2.13
    //     */
    //    @TechnicalPreview(since = "3.2.13")
    //    Class<? extends PreHandlerChain> preHandlerChain() default PreHandlerChain.class;
}