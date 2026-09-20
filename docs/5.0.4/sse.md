# SSE 流式响应

`KeelWebSseReceptionist` 直接继承 `KeelWebReceptionist`，由 `KeelWebSseStream` 管理响应。
可通过现有 `KeelWebReceptionistLoader.loadClass` 注册；若按包扫描，需要使用
`KeelWebReceptionist.class` 或 `KeelWebSseReceptionist.class` 作为扫描基类。

## 有限流示例

```java
import io.github.sinri.keel.web.http.receptionist.*;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

@ApiMeta(routePath = "/events", allowMethods = {"GET"},
        requestBodyNeeded = false, timeout = 0)
public final class EventsReceptionist extends KeelWebSseReceptionist {
    public EventsReceptionist(RoutingContext context) {
        super(context);
    }

    @Override
    protected Future<Void> handleStream(KeelWebSseStream stream) {
        return stream.sendEvent(new KeelWebSseEvent().data("第一段"))
                .compose(v -> stream.sendEvent(new KeelWebSseEvent().data("第二段")))
                .compose(v -> stream.sendEvent(new KeelWebSseEvent()
                        .data("{}").event("done")));
    }
}
```

返回的 Future 成功后，基类结束响应。业务也可以主动调用幂等的 `end()`。
原生浏览器客户端示例：

```javascript
const events = new EventSource('/events');
events.onmessage = e => console.log(e.data);
events.addEventListener('done', () => events.close());
events.addEventListener('error', e => {
  console.error(JSON.parse(e.data));
  events.close();
});
```

服务端结束连接后 EventSource 通常会自动重连，因此有限业务使用 `done` 事件让客户端关闭。
`done` 是业务约定，框架不自动发送。POST 请求也可返回 SSE，但客户端需要使用 fetch
读取并解析事件流，不能使用原生 EventSource 发送 POST。

## 生产、背压与取消

- `handleStream` 的 Future 表示整个生产过程结束，不能在仅注册订阅后立即返回成功。
- 按 `sendEvent(...).compose(...)` 串联输出；发送直接返回 response.write 的 Future，不额外等待 drain，也不表示客户端已消费。
- 不支持并发调用。由调用方串联发送 Future，框架不维护发送队列或检查重叠发送。
- 必须等待最后一次发送完成后再调用 `end()`；框架不再检查是否仍有发送进行中。
- `stream.completion().onComplete(...)` 用于释放订阅、定时器及上游请求。正常关闭成功，
  断连或写出失败时失败。上游取消需要业务显式实现。
- 流独占 HTTP response 的写入与 close/exception 回调，业务不要覆盖这些回调或直接写 response。
- 没有内部发送队列、单事件大小限制或写出超时定时器；调用方负责控制事件大小及业务超时。

心跳由业务放入同一条发送链，框架不启动后台定时器，避免与业务发送竞争：

```java
return stream.sendEvent(new KeelWebSseEvent().data("第一段"))
        .compose(v -> stream.sendComment("heartbeat"))
        .compose(v -> stream.sendEvent(new KeelWebSseEvent().data("第二段")));
```

桥接推送源时，应让源等待每次发送的 Future，并在 completion 回调中取消源和结束业务生产 Promise。

## 配置与错误

`prepare()` 在响应提交前运行，可异步校验参数和资源。失败交给路由 failure handler。
框架不限制准备阶段的等待时间；业务自行控制 prepare 内部操作的超时、取消和资源释放。

开流后业务失败时发送通用 `error` 事件（包含 request_id），然后关闭；详细异常仅记录日志。
传输失败或断连时不保证错误事件能够到达。

HTTP/1.1 使用分块响应，HTTP/2 由 Vert.x 负责帧传输。
设置 `Content-Type: text/event-stream; charset=utf-8`、`Cache-Control: no-cache, no-transform`
及 `X-Accel-Buffering: no`。部署时还需匹配代理缓冲、压缩和空闲超时配置。
SSE 路由需要 `@ApiMeta(timeout = 0)`，否则会受到默认 10 秒请求超时影响。

事件使用 UTF-8；多行 data 逐行编码；event/id 禁止换行，id 禁止 NUL。
可选字段不赋值时省略；赋值方法不接受 null。空 id 重置游标。retry 接受非负 long 毫秒值。
框架不保存、重放事件；需要断点恢复时，由业务读取 `Last-Event-ID` 并实现存储与去重。

## 注释事件

`ServerSentEvent` 支持纯注释及附带注释的数据事件，注释支持多行：

```java
stream.sendEvent(new ServerSentEvent().comment("heartbeat"));
stream.sendEvent(new ServerSentEvent().comment("说明").data("内容"));
```

纯注释无需设置 data，编码为 `: heartbeat\n\n`。`sendComment()` 复用这一编码逻辑。
