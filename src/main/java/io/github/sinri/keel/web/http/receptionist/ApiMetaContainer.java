package io.github.sinri.keel.web.http.receptionist;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 使{@link ApiMeta}注解可以多个共存的容器注解。
 * @since 5.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMetaContainer {
    ApiMeta[] value();
}
