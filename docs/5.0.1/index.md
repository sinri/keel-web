---
title: Keel-Web 5.0.1 用户使用文档
---

# Keel-Web 5.0.1 用户使用文档

Keel-Web 是 [Keel](https://github.com/sinri/keel-web) 框架的 Web 功能组件，基于 [Vert.x 5](https://vertx.io/) 构建，提供 HTTP 服务端、HTTP 客户端、TCP 和 UDP 网络通信等能力。

## 模块依赖

```
io.github.sinri.keel.web
├── io.github.sinri.keel.base       (Keel 基础模块)
├── io.github.sinri.keel.core       (Keel 核心模块)
├── io.github.sinri.keel.logger.api (Keel 日志模块)
├── io.vertx.core                   (Vert.x Core 5.0.8)
├── io.vertx.web                    (Vert.x Web)
├── io.vertx.web.client             (Vert.x Web Client)
└── io.vertx.auth.common            (Vert.x Auth)
```

## Gradle 引入

```groovy
dependencies {
    implementation 'io.github.sinri:keel-web:5.0.1'
}
```

## 功能模块一览

| 模块       | 包路径                                                    | 说明                | 文档                                 |
|----------|--------------------------------------------------------|-------------------|------------------------------------|
| HTTP 服务  | `io.github.sinri.keel.web.http`                        | HTTP 服务器基础类       | [http-server.md](http-server.md)   |
| 请求接待     | `io.github.sinri.keel.web.http.receptionist`           | 请求路由与处理           | [receptionist.md](receptionist.md) |
| 预处理器     | `io.github.sinri.keel.web.http.prehandler`             | 请求预处理链（认证、授权等）    | [prehandler.md](prehandler.md)     |
| 响应器      | `io.github.sinri.keel.web.http.receptionist.responder` | 请求响应输出            | [responder.md](responder.md)       |
| HTTP 客户端 | `io.github.sinri.keel.web.http.requester`              | HTTP 响应萃取与错误处理    | [requester.md](requester.md)       |
| FastDocs | `io.github.sinri.keel.web.http.fastdocs`               | 基于 Markdown 的文档系统 | [fastdocs.md](fastdocs.md)         |
| TCP 通信   | `io.github.sinri.keel.web.tcp`                         | TCP Socket 封装     | [tcp.md](tcp.md)                   |
| UDP 通信   | `io.github.sinri.keel.web.udp`                         | UDP 数据报收发         | [udp.md](udp.md)                   |

## 快速开始

以下示例展示如何创建一个最基本的 HTTP 服务：

```java
public class MyHttpServer extends KeelHttpServer {
    @Override
    protected void configureRoutes(Router router) {
        KeelWebReceptionistLoader.loadPackage(
                router,
                "com.example.api",
                KeelWebFutureReceptionist.class,
                getHttpServerLogger()
        );
    }
}
```

启动服务：

```java
Keel keel = ...;
MyHttpServer server = new MyHttpServer();
server.

deployMe(keel);
```

编写一个接口处理类：

```java

@ApiMeta(routePath = "/api/hello", allowMethods = {"GET"})
public class HelloReceptionist extends KeelWebFutureReceptionist<JsonObject> {
    public HelloReceptionist(RoutingContext routingContext) {
        super(routingContext);
    }

    @Override
    protected KeelWebResponder<JsonObject> buildResponder() {
        return KeelWebResponder.createCommonInstance(getRoutingContext(), getLogger());
    }

    @Override
    protected Future<JsonObject> handleForFuture() {
        return Future.succeededFuture(new JsonObject().put("message", "Hello, World!"));
    }
}
```

## 核心设计理念

- **注解驱动路由**：通过 `@ApiMeta` 注解声明接口的路径、方法、超时等元信息，由 `KeelWebReceptionistLoader` 自动扫描并注册路由。
- **预处理器链**：通过 `PreHandlerChain` 和 `@PreHandlerChainMeta` 注解实现灵活的请求预处理（认证、授权、CORS 等）。
- **响应器模式**：将响应逻辑与业务逻辑分离，通过 `KeelWebResponder` 接口统一处理成功和失败的响应输出。
- **虚拟线程支持**：自动检测虚拟线程可用性，优先使用虚拟线程部署 Verticle；提供
  `KeelWebFutureReceptionistInVirtualThread` 以支持在虚拟线程中编写同步风格的接口处理逻辑。

## 许可证

GPL-v3.0
