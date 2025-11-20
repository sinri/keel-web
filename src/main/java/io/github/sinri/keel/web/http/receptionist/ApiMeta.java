package io.github.sinri.keel.web.http.receptionist;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 针对一个请求接待类的接口元信息注解。
 * <p>
 * 通过{@link ApiMetaContainer}，同一个请求接待类可以同时加上多个本注解。
 *
 * @since 5.0.0
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
     */
    long timeout() default 10_000;

    /**
     * @return the HTTP RESPONSE STATUS CODE for timeout.
     */
    int statusCodeForTimeout() default 509;

    /**
     * @return It this path deprecated.
     */
    boolean isDeprecated() default false;

    String remark() default "";
}