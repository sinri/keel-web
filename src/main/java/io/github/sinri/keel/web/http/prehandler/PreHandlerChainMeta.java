package io.github.sinri.keel.web.http.prehandler;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 本注解指示了一个相应的请求接待类应经过哪一个预处理器链进行预处理。
 *
 * @since 5.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PreHandlerChainMeta {
    Class<? extends PreHandlerChain> value() default PreHandlerChain.class;
}
