# SSE 流式响应

`KeelWebSseReceptionist` 是轻量工具基类，提供 `setHeadersForSSE()` 和 `pushOneEvent()`。
子类实现 `handle()`，负责校验、设置响应头、串联发送、结束响应及异常处理。
可通过现有 `KeelWebReceptionistLoader` 注册。

```java
import io.github.sinri.keel.web.http.receptionist.*;
import io.github.sinri.keel.web.http.receptionist.sse.ServerSentEvent;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

@ApiMeta(routePath = "/events", allowMethods = {"GET"},
        requestBodyNeeded = false, timeout = 0)
public final class EventsReceptionist extends KeelWebSseReceptionist {
    public EventsReceptionist(RoutingContext context) {
        super(context);
    }

    @Override
    public void handle() {
        var ctx = getRoutingContext();
        Future.succeededFuture()
                .compose(v -> {
                    setHeadersForSSE(ctx);
                    return pushOneEvent(new ServerSentEvent().comment("connected"));
                })
                .compose(v -> pushOneEvent(new ServerSentEvent().data("内容")))
                .compose(v -> pushOneEvent(new ServerSentEvent().event("done").data("{}")))
                .compose(v -> ctx.response().end())
                .onFailure(error -> {
                    getLogger().error(log -> log.exception(error));
                    if (ctx.response().closed()) return;
                    if (!ctx.response().headWritten()) {
                        ctx.fail(error);
                    } else {
                        ctx.response().reset().onFailure(resetError ->
                                getLogger().error(log -> log.exception(resetError)));
                    }
                });
    }
}
```

`pushOneEvent()` 将编码异常、同步写入异常及异步写入失败统一放入返回的 Future。
该方法不会设置响应头、自动结束响应、发送错误事件或取消上游任务。
调用方应串联发送 Future 并观察失败，不要并发发送或忽略结果。
写出成功不代表客户端已经消费，也不额外等待 drain。

业务需要自行注册 response 的 close/exception 回调，取消订阅、定时器或上游请求。
基类没有自动心跳、准备阶段超时、完成通知或内部发送队列。
长连接路由使用 `@ApiMeta(timeout = 0)`；业务自行控制操作超时及资源释放。

`ServerSentEvent` 使用链式赋值：

```java
new ServerSentEvent().data("内容").event("delta").id("42").retry(1000);
new ServerSentEvent().comment("heartbeat");
new ServerSentEvent().comment("说明").data("内容");
```

纯注释无需 data；注释和 data 都支持多行。未设置的可选字段不输出，赋值方法不接受 null。
event/id 禁止换行，id 禁止 NUL；空 id 重置客户端游标。至少设置 data 或 comment。
事件是可变对象，编码和发送时不要并发修改。

客户端可使用 EventSource；有限任务收到业务定义的 `done` 事件后应主动关闭：

```javascript
const events = new EventSource('/events');
events.onmessage = e => console.log(e.data);
events.addEventListener('done', () => events.close());
```

框架不自动发送 done 或 error 事件，也不存储或重放事件。
需要恢复时由业务读取 `Last-Event-ID` 并实现重放、去重。
POST 请求可以返回 SSE，但客户端应使用 fetch 读取解析，原生 EventSource 不支持 POST。

HTTP/1.1 使用分块响应；HTTP/2 帧传输由 Vert.x 处理。
部署时还需匹配代理缓冲、压缩及空闲超时配置。
