---
title: HTTP 服务 - KeelHttpServer
---

# HTTP 服务 - KeelHttpServer

`KeelHttpServer` 是 Keel-Web 中 HTTP 服务的核心抽象类，继承自 `KeelVerticleBase`，以 Vert.x Verticle 的形式运行 HTTP 服务。

## 类结构

```
KeelHttpServer extends KeelVerticleBase
```

## 基本用法

### 1. 创建服务子类

继承 `KeelHttpServer` 并实现 `configureRoutes` 方法来配置路由：

```java
public class MyHttpServer extends KeelHttpServer {
    @Override
    protected void configureRoutes(Router router) {
        // 手动注册路由
        router.get("/health").handler(ctx -> ctx.response().end("OK"));

        // 自动扫描并注册接口类
        KeelWebReceptionistLoader.loadPackage(
            router,
            "com.example.api",
            KeelWebFutureReceptionist.class,
            getHttpServerLogger()
        );
    }
}
```

### 2. 部署服务

```java
Keel keel = ...;
MyHttpServer server = new MyHttpServer();
server.deployMe(keel)
      .onSuccess(deploymentId -> System.out.println("Server started"))
      .onFailure(Throwable::printStackTrace);
```

`deployMe(Keel)` 方法会自动检测虚拟线程可用性，若可用则以虚拟线程模型部署。

## 配置

### 端口配置

默认端口为 `8080`，可通过 Verticle 的 config JSON 进行配置：

| 配置项                   | 类型         | 默认值  | 说明                           |
|-----------------------|------------|------|------------------------------|
| `http_server_port`    | Integer    | 8080 | HTTP 服务监听端口                  |
| `http_server_options` | JsonObject | —    | 完整的 `HttpServerOptions` 配置对象 |

配置示例：

```java
DeploymentOptions options = new DeploymentOptions()
    .setConfig(new JsonObject().put("http_server_port", 9090));
keel.deployVerticle(server, options);
```

如需更精细的控制，可提供完整的 `HttpServerOptions` JSON：

```java
new JsonObject().put("http_server_options", new JsonObject()
    .put("port", 9090)
    .put("host", "0.0.0.0")
    .put("maxWebSocketFrameSize", 65536)
);
```

### 自定义 HttpServerOptions

也可覆写 `getHttpServerOptions()` 方法：

```java
@Override
private HttpServerOptions getHttpServerOptions() {
    return new HttpServerOptions()
        .setPort(9090)
        .setSsl(true)
        .setKeyCertOptions(...);

```

## 生命周期钩子

| 方法                      | 说明                             |
|-------------------------|--------------------------------|
| `beforeStartServer()`   | 服务启动前执行的准备工作，返回 `Future<Void>` |
| `afterShutdownServer()` | 服务关闭后执行的清理工作，返回 `Future<Void>` |

```java
@Override
private Future<Void> beforeStartServer() {
    // 初始化数据库连接池等
    return initDatabase();
}

@Override
protected Future<Void> afterShutdownServer() {
    // 释放资源
    return closeDatabase();
```

## 日志

通过 `getHttpServerLogger()` 获取服务级别的日志记录器，主题为 `"KeelHttpServer"`。

```java
getHttpServerLogger().info(r -> r.message("Custom log message"));
```
