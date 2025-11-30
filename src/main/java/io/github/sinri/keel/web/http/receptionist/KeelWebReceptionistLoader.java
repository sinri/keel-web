package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.core.utils.ReflectionUtils;
import io.github.sinri.keel.logger.api.logger.Logger;
import io.github.sinri.keel.web.http.prehandler.PreHandlerChain;
import io.github.sinri.keel.web.http.prehandler.PreHandlerChainMeta;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.sinri.keel.base.KeelInstance.Keel;


/**
 * 请求接待类加载器。
 * <p>
 * 这是一个工具类。
 *
 * @since 5.0.0
 */
public final class KeelWebReceptionistLoader {

    /**
     * 自某个包中加载指定类型的请求接待类，基于各类的接口元信息登记到路由中。
     * <p>
     * Note: MAIN and TEST scopes are separated.
     *
     * @param router              路由
     * @param packageName         请求接待类所在的包；扫描时包含其下的子包。
     * @param classOfReceptionist 具体要通过反射支持的特定的一类请求接待类的基类
     * @param <R>                 具体要通过反射支持的特定的请求接待类的类型。
     */
    static <R extends KeelWebReceptionist> void loadPackage(
            @NotNull Router router,
            @NotNull String packageName,
            @NotNull Class<R> classOfReceptionist
    ) {
        Set<Class<? extends R>> allClasses = ReflectionUtils.seekClassDescendantsInPackage(packageName, classOfReceptionist);

        try {
            allClasses.forEach(c -> loadClass(router, c));
        } catch (Exception e) {
            Keel.getLoggerFactory()
                .createLogger(KeelWebReceptionistLoader.class.getName())
                .error(log -> log
                        .classification(List.of("KeelWebReceptionistLoader", "loadPackage"))
                        .exception(e)
                );
        }
    }

    /**
     * 加载的请求接待类，基于各类的接口元信息登记到路由中。
     *
     * @param router 路由
     * @param c      具体要通过反射支持的请求接待类
     * @param <R>    具体要通过反射支持的请求接待类的类型。
     */
    static <R extends KeelWebReceptionist> void loadClass(@NotNull Router router,@NotNull Class<? extends R> c) {
        ApiMeta[] apiMetaArray = ReflectionUtils.getAnnotationsOfClass(c, ApiMeta.class);
        for (var apiMeta : apiMetaArray) {
            loadClass(router, c, apiMeta);
        }
    }

    private static <R extends KeelWebReceptionist> void loadClass(@NotNull Router router, @NotNull Class<? extends R> c, ApiMeta apiMeta) {
        Logger logger = Keel.getLoggerFactory()
                            .createLogger(KeelWebReceptionistLoader.class.getName());
        logger.info(r -> r
                .classification(List.of("KeelWebReceptionistLoader", "loadClass"))
                .message("Loading " + c.getName())
                .context(j -> {
                    JsonArray methods = new JsonArray();
                    Arrays.stream(apiMeta.allowMethods()).forEach(methods::add);
                    j.put("allowMethods", methods);
                    j.put("routePath", apiMeta.routePath());
                    if (apiMeta.isDeprecated()) {
                        j.put("isDeprecated", true);
                    }
                    if (!apiMeta.remark().isEmpty()) {
                        j.put("remark", apiMeta.remark());
                    }
                })
        );

        Constructor<? extends R> receptionistConstructor;
        try {
            receptionistConstructor = c.getConstructor(RoutingContext.class);
        } catch (NoSuchMethodException e) {
            logger.error(r -> r.classification(List.of("KeelWebReceptionistLoader", "loadClass"))
                               .message("HANDLER REFLECTION EXCEPTION")
                               .exception(e));
            return;
        }

        Route route = router.route(apiMeta.routePath());

        if (apiMeta.allowMethods() != null) {
            for (var methodName : apiMeta.allowMethods()) {
                route.method(HttpMethod.valueOf(methodName));
            }
        }

        if (!apiMeta.virtualHost().isEmpty()) {
            route.virtualHost(apiMeta.virtualHost());
        }

        AtomicReference<Class<?>> classRef = new AtomicReference<>(c);

        PreHandlerChain preHandlerChain = null;

        while (true) {
            Class<?> child = classRef.get();
            if (child == null) {
                break;
            }
            if (child == KeelWebReceptionist.class) {
                break;
            }
            PreHandlerChainMeta annotation = child.getAnnotation(PreHandlerChainMeta.class);
            if (annotation != null) {
                Class<? extends PreHandlerChain> preHandlerChainClass = annotation.value();
                try {
                    preHandlerChain = preHandlerChainClass.getConstructor().newInstance();
                } catch (Throwable e) {
                    logger.error(r -> r.classification(List.of("KeelWebReceptionistLoader", "loadClass"))
                                       .message("PreHandlerChain REFLECTION EXCEPTION")
                                       .exception(e));
                    return;
                }
                break;
            }

            Class<?> superclass = child.getSuperclass();
            classRef.set(superclass);
        }

        if (preHandlerChain == null) {
            preHandlerChain = new PreHandlerChain();
        }
        preHandlerChain.executeHandlers(route, apiMeta);

        // finally!
        route.handler(routingContext -> {
            try {
                R receptionist = receptionistConstructor.newInstance(routingContext);
                receptionist.handle();
            } catch (Throwable e) {
                routingContext.fail(e);
            }
        });

    }

}
