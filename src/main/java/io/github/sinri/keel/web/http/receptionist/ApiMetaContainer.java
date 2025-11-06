package io.github.sinri.keel.web.http.receptionist;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @since 3.2.12
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMetaContainer {
    ApiMeta[] value();
}
