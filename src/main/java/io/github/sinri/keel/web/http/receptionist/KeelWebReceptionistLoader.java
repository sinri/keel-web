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

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.sinri.keel.base.KeelInstance.Keel;


/**
 * @since 3.2.13
 * @since 3.2.18 from class to interface.
 */
public interface KeelWebReceptionistLoader {

    /**
     * Note: MAIN and TEST scopes are seperated.
     *
     * @param packageName the name of the package where the classes extending `R` are.
     */
    static <R extends KeelWebReceptionist> void loadPackage(Router router, String packageName, Class<R> classOfReceptionist) {
        Set<Class<? extends R>> allClasses = ReflectionUtils.seekClassDescendantsInPackage(packageName, classOfReceptionist);

        try {
            allClasses.forEach(c -> loadClass(router, c));
        } catch (Exception e) {
            Keel.getLoggerFactory()
                .createLogger(KeelWebReceptionistLoader.class.getName())
                .exception(e, r -> r.classification(List.of("KeelWebReceptionistLoader", "loadPackage")));
        }
    }

    static <R extends KeelWebReceptionist> void loadClass(Router router, Class<? extends R> c) {
        ApiMeta[] apiMetaArray = ReflectionUtils.getAnnotationsOfClass(c, ApiMeta.class);
        for (var apiMeta : apiMetaArray) {
            loadClass(router, c, apiMeta);
        }
    }

    private static <R extends KeelWebReceptionist> void loadClass(Router router, Class<? extends R> c, ApiMeta apiMeta) {
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
                    if (apiMeta.remark() != null && !apiMeta.remark().isEmpty()) {
                        j.put("remark", apiMeta.remark());
                    }
                })
        );

        Constructor<? extends R> receptionistConstructor;
        try {
            receptionistConstructor = c.getConstructor(RoutingContext.class);
        } catch (NoSuchMethodException e) {
            logger.exception(e, r -> r.classification(List.of("KeelWebReceptionistLoader", "loadClass"))
                                                .message("HANDLER REFLECTION EXCEPTION"));
            return;
        }

        Route route = router.route(apiMeta.routePath());

        if (apiMeta.allowMethods() != null) {
            for (var methodName : apiMeta.allowMethods()) {
                route.method(HttpMethod.valueOf(methodName));
            }
        }

        if (apiMeta.virtualHost() != null && !apiMeta.virtualHost().isEmpty()) {
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
                    logger.exception(e, r -> r.classification(List.of("KeelWebReceptionistLoader", "loadClass"))
                                                        .message("PreHandlerChain REFLECTION EXCEPTION"));
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
