---
title: 请求接待 - Receptionist
---

# 请求接待 - Receptionist

请求接待（Receptionist）是 Keel-Web 中处理 HTTP 请求的核心机制。每个接口对应一个 Receptionist 类，通过注解声明路由元信息，由框架自动扫描并注册。

## 类层次

```
KeelWebReceptionist                          (抽象基类)
├── KeelWebFutureReceptionist<R>             (异步 Future 模式)
│   └── KeelWebFutureReceptionistInVirtualThread<R>  (虚拟线程模式)
```

## KeelWebReceptionist

所有请求接待类的基类，封装了 `RoutingContext` 并提供常用工具方法。

### 核心方法

| 方法                          | 说明                                 |
|-----------------------------|------------------------------------|
| `handle()`                  | 抽象方法，处理请求的主入口                      |
| `getRoutingContext()`       | 获取 Vert.x `RoutingContext`         |
| `getVertx()`                | 获取 `Vertx` 实例                      |
| `getKeel()`                 | 获取 `Keel` 实例                       |
| `getLogger()`               | 获取请求级别的日志记录器                       |
| `readRequestID()`           | 读取由 `KeelPlatformHandler` 分配的请求 ID |
| `readRequestStartTime()`    | 读取请求开始处理的时间戳（毫秒）                   |
| `readRequestIPChain()`      | 解析请求的 IP 链（含 X-Forwarded-For）      |
| `readRequestUser()`         | 获取请求上下文中的已认证用户                     |
| `readFirstQueryValue(name)` | 读取指定 Query 参数的第一个值                 |

### Cookie 操作

```java
addCookie("token", "abc123", "/", 3600L, true);
removeCookie("token");
```

### 详细日志模式

覆写 `isVerboseLogging()` 返回 `true` 可启用 DEBUG 级别的日志，包括请求 Query 和 Body 的记录。

## KeelWebFutureReceptionist\<R\>

基于异步 Future 模式的请求接待类，推荐用于大多数场景。

### 用法

```java
@ApiMeta(routePath = "/api/users", allowMethods = {"GET"})
public class ListUsersReceptionist extends KeelWebFutureReceptionist<JsonObject> {
    public ListUsersReceptionist(RoutingContext routingContext) {
        super(routingContext);
    }

    @Override
    protected KeelWebResponder<JsonObject> buildResponder() {
        return KeelWebResponder.createCommonInstance(getRoutingContext(), getLogger());
    }

    @Override
    protected Future<JsonObject> handleForFuture() {
        // 异步业务逻辑
        return queryUsers()
            .map(users -> new JsonObject().put("users", users));
    }
}
```

### 处理流程

1. 构造时自动调用 `buildResponder()` 创建响应器
2. `handle()` 方法内部调用 `handleForFuture()` 获取异步结果
3. 成功时通过 `responder.respondOnSuccess(result)` 输出
4. 失败时，若异常为 `KeelWebApiError` 则直接传递，否则自动包装为 500 错误

## KeelWebFutureReceptionistInVirtualThread\<R\>

专为虚拟线程环境设计的请求接待类。允许在虚拟线程中以同步风格编写代码，同时保持非阻塞特性。

### 用法

```java
@ApiMeta(routePath = "/api/user/:id", allowMethods = {"GET"})
public class GetUserReceptionist extends KeelWebFutureReceptionistInVirtualThread<JsonObject> {
    public GetUserReceptionist(RoutingContext routingContext) {
        super(routingContext);
    }

    @Override
    protected KeelWebResponder<JsonObject> buildResponder() {
        return KeelWebResponder.createCommonInstance(getRoutingContext(), getLogger());
    }

    @Override
    protected JsonObject handleInVirtualThread() throws KeelWebApiError {
        String userId = readFirstQueryValue("id");
        if (userId == null) {
            throw new KeelWebApiError(400, "Missing user id", null);
        }
        // 可使用 await 调用异步方法
        JsonObject user = await(queryUserById(userId));
        return new JsonObject().put("user", user);
    }
}
```

### 与 KeelWebFutureReceptionist 的区别

| 特性   | KeelWebFutureReceptionist          | KeelWebFutureReceptionistInVirtualThread |
|------|------------------------------------|------------------------------------------|
| 主方法  | `handleForFuture()` 返回 `Future<R>` | `handleInVirtualThread()` 返回 `R`         |
| 编码风格 | 异步链式                               | 同步风格                                     |
| 错误处理 | Future failure                     | 抛出异常                                     |
| 运行环境 | Event Loop / 虚拟线程                  | 虚拟线程                                     |

## @ApiMeta 注解

声明请求接待类的路由元信息。支持通过 `@Repeatable` 在同一个类上多次使用。

| 属性                     | 类型       | 默认值        | 说明                      |
|------------------------|----------|------------|-------------------------|
| `routePath`            | String   | —          | 路由路径（必填），如 `/api/hello` |
| `allowMethods`         | String[] | `{"POST"}` | 允许的 HTTP 方法             |
| `virtualHost`          | String   | `""`       | 虚拟主机匹配规则                |
| `requestBodyNeeded`    | boolean  | `true`     | 是否需要解析请求体               |
| `timeout`              | long     | `10000`    | 超时时间（毫秒），0 表示不限         |
| `statusCodeForTimeout` | int      | `509`      | 超时时返回的 HTTP 状态码         |
| `isDeprecated`         | boolean  | `false`    | 标记接口是否已废弃               |
| `remark`               | String   | `""`       | 接口备注                    |

### 多路径绑定

```java
@ApiMeta(routePath = "/api/v1/hello", allowMethods = {"GET"})
@ApiMeta(routePath = "/api/v2/hello", allowMethods = {"GET"})
public class HelloReceptionist extends KeelWebFutureReceptionist<JsonObject> {
    // ...
}
```

## KeelWebReceptionistLoader

工具类，负责扫描和注册请求接待类。

### 按包扫描

```java
KeelWebReceptionistLoader.loadPackage(
    router,
    "com.example.api",           // 包名（含子包）
    KeelWebFutureReceptionist.class,  // 基类类型
    logger
);
```

### 按类注册

```java
KeelWebReceptionistLoader.loadClass(router, HelloReceptionist.class, logger);
```

加载过程会：

1. 读取类上的 `@ApiMeta` 注解
2. 查找类层级上的 `@PreHandlerChainMeta` 注解以确定预处理器链
3. 注册预处理器链中的各处理器
4. 注册最终的请求处理器

## AbstractRequestBody

用于从请求中解析请求体的工具基类，自动支持 JSON (`application/json`) 和表单 (`application/x-www-form-urlencoded`,
`multipart/form-data`) 格式。

### 用法

```java
public class CreateUserRequestBody extends AbstractRequestBody {
    public CreateUserRequestBody(RoutingContext routingContext) {
        super(routingContext);
    }

    public String getName() {
        return readString("name");
    }

    public String getEmail() {
        return readString("email");
    }
}
```

在接口处理类中使用：

```java
@Override
private Future<JsonObject> handleForFuture() {
    CreateUserRequestBody body = new CreateUserRequestBody(getRoutingContext());
    String name = body.getName();
    // ...
}
```
