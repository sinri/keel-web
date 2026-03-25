---
title: 响应器 - Responder
---

# 响应器 - Responder

响应器（Responder）负责将请求处理结果格式化并输出到 HTTP 响应中。它将响应逻辑与业务逻辑分离，提供统一的成功与失败响应处理。

## 类层次

```
KeelWebResponder<R>                    (接口)
└── AbstractKeelWebResponder<R>        (抽象基类)
    └── KeelWebResponderCommonApiImpl  (标准 JSON API 实现)
```

## KeelWebResponder\<R\> 接口

| 方法                                               | 说明               |
|--------------------------------------------------|------------------|
| `respondOnSuccess(R data)`                       | 成功时输出响应          |
| `respondOnFailure(KeelWebApiError)`              | 失败时输出响应          |
| `respondOnFailure(KeelWebApiError, ValueBox<?>)` | 失败时输出响应，附带额外数据   |
| `isVerboseLogging()`                             | 是否启用详细日志         |
| `contentTypeToRespond()`                         | 返回头 Content-Type |

### 快速创建标准实例

```java
KeelWebResponder<JsonObject> responder = KeelWebResponder.createCommonInstance(
    routingContext,
    logger
);
```

## 标准 JSON API 响应格式

`KeelWebResponderCommonApiImpl` 提供了统一的 JSON API 响应格式：

### 成功响应

```json
{
    "request_id": "192.168.1.1-42-550e8400-e29b-41d4-a716-446655440000",
    "code": "OK",
    "data": { ... }
}
```

### 失败响应

```json
{
    "request_id": "192.168.1.1-42-550e8400-e29b-41d4-a716-446655440000",
    "code": "FAILED",
    "data": {
        "extra": ...
    },
    "throwable": {
        "class": "io.github.sinri.keel.web.http.receptionist.responder.KeelWebApiError",
        "message": "...",
        "stackTrace": [...]
    }
}
```

- Content-Type 为 `application/json`
- 当 `KeelWebApiError` 的 `statusCode` 不为 200 时，HTTP 状态码会设置为对应值

## AbstractKeelWebResponder\<R\>

响应器的抽象基类，提供通用能力。

### 可用方法

| 方法                                | 说明             |
|-----------------------------------|----------------|
| `getRoutingContext()`             | 获取路由上下文        |
| `getLogger()`                     | 获取日志记录器        |
| `readRequestID()`                 | 读取请求 ID        |
| `readRequestStartTime()`          | 读取请求开始时间       |
| `readRequestIPChain()`            | 读取请求 IP 链      |
| `recordResponseVerbosely(Object)` | 在详细日志模式下记录响应内容 |

## KeelWebApiError

Web API 错误类，用于在请求处理过程中抛出业务错误。

### 构造

```java
// 默认 HTTP 200，仅包含错误消息（业务逻辑层面的错误，通过 code=FAILED 标识）
throw new KeelWebApiError("User not found");

// 指定 HTTP 状态码
throw new KeelWebApiError(404, "User not found", null);

// 包装已有异常
throw new KeelWebApiError(500, "Database error", cause);
```

### 自动包装

当 `KeelWebFutureReceptionist` 的 `handleForFuture()` 抛出非 `KeelWebApiError` 异常时，框架会自动调用
`KeelWebApiError.wrap(throwable)` 将其包装为 500 错误。

## 自定义响应器

如需自定义响应格式（如 XML、HTML 等），实现 `KeelWebResponder<R>` 接口或继承 `AbstractKeelWebResponder<R>`：

```java
public class HtmlResponder extends AbstractKeelWebResponder<String> {
    public HtmlResponder(RoutingContext routingContext, SpecificLogger<ReceptionistSpecificLog> logger) {
        super(routingContext, logger);
    }

    @Override
    public void respondOnSuccess(String html) {
        getRoutingContext().response()
            .putHeader(HttpHeaders.CONTENT_TYPE, contentTypeToRespond())
            .end(html);
    }

    @Override
    public void respondOnFailure(KeelWebApiError webApiError, ValueBox<?> dataValueBox) {
        getRoutingContext().response()
            .setStatusCode(webApiError.getStatusCode())
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
            .end(webApiError.getMessage());
    }

    @Override
    public String contentTypeToRespond() {
        return "text/html";
    }
}
```

然后在接口处理类中使用：

```java
@Override
private KeelWebResponder<String> buildResponder() {
    return new HtmlResponder(getRoutingContext(), getLogger());
}

@Override
protected Future<String> handleForFuture() {
    return Future.succeededFuture("<html><body>Hello!</body></html>");
```
