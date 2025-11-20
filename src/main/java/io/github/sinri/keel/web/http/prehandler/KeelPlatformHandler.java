package io.github.sinri.keel.web.http.prehandler;

import io.github.sinri.keel.core.utils.NetUtils;
import io.vertx.core.Future;
import io.vertx.core.shareddata.Counter;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.PlatformHandler;

import java.util.Random;
import java.util.UUID;

import static io.github.sinri.keel.base.KeelInstance.Keel;


/**
 * 平台级处理器。
 * <p>
 * 向{@link RoutingContext}的数据记录中添加请求 ID 和请求到达时间。
 *
 * @since 5.0.0
 */
public class KeelPlatformHandler implements PlatformHandler {
    public final static String KEEL_REQUEST_ID = "KEEL_REQUEST_ID"; // -> String
    public final static String KEEL_REQUEST_START_TIME = "KEEL_REQUEST_START_TIME"; // -> long * 0.001 second


    @Override
    public void handle(RoutingContext routingContext) {
        // BEFORE ASYNC PAUSE
        routingContext.request().pause();
        // START !
        Keel.getVertx().sharedData()
            .getCounter("KeelPlatformHandler-RequestID-Counter")
            .compose(Counter::incrementAndGet)
            .recover(throwable -> Future.succeededFuture(new Random().nextLong() * -1))
            .compose(id -> {
                routingContext.put(KEEL_REQUEST_ID, "%s-%s-%s".formatted(NetUtils.getLocalHostAddress(), id, UUID.randomUUID()));

                routingContext.put(KEEL_REQUEST_START_TIME, System.currentTimeMillis());

                return Future.succeededFuture();
            })
            .andThen(v -> {
                // RESUME
                routingContext.request().resume();
                // NEXT !
                routingContext.next();
            });
    }
}
