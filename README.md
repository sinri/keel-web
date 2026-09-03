# Keel-Web

Keel-Web 是 Keel 的 Web 与网络通信组件，基于 Vert.x 5，提供 HTTP 服务端、注解路由、请求预处理、统一响应、HTTP 响应萃取、FastDocs、TCP 和 UDP 支持。

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)

## 环境与版本

| 组件 | 版本 |
|---|---|
| Keel-Web | 5.0.3 |
| Java | 17+ |
| Vert.x | 5.1.3 |
| Keel Core | 5.0.3 |

项目是具名 Java 模块，模块名为 `io.github.sinri.keel.web`。

## 安装

Gradle Kotlin DSL：

```kotlin
dependencies {
    implementation("io.github.sinri:keel-web:5.0.3")
}
```

Gradle Groovy DSL：

```groovy
dependencies {
    implementation 'io.github.sinri:keel-web:5.0.3'
}
```

Maven：

```xml
<dependency>
  <groupId>io.github.sinri</groupId>
  <artifactId>keel-web</artifactId>
  <version>5.0.3</version>
</dependency>
```

## HTTP 服务快速开始

继承 `KeelHttpServer` 配置 Vert.x 路由：

```java
public final class MyHttpServer extends KeelHttpServer {
    @Override
    protected void configureRoutes(Router router) {
        router.get("/health")
              .handler(ctx -> ctx.response().end("OK"));

        KeelWebReceptionistLoader.loadPackage(
                router,
                "com.example.api",
                KeelWebFutureReceptionist.class,
                getHttpServerLogger()
        );
    }
}
```

部署服务：

```java
Keel keel = ...;
new MyHttpServer().deployMe(keel)
                  .onSuccess(id -> System.out.println("HTTP server deployed: " + id))
                  .onFailure(Throwable::printStackTrace);
```

一个注解路由处理器：

```java

@ApiMeta(routePath = "/api/hello", allowMethods = {"GET"}, requestBodyNeeded = false)
public final class HelloReceptionist extends KeelWebFutureReceptionist<JsonObject> {
    public HelloReceptionist(RoutingContext context) {
        super(context);
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

## 功能模块

| 模块              | 主要类型 | 文档 |
|-------------------|---|---|
| HTTP 服务         | `KeelHttpServer` | [HTTP 服务](docs/5.0.3/http-server.md) |
| 注解路由与请求体  | `KeelWebReceptionistLoader`, `AbstractRequestBody` | [请求接待](docs/5.0.3/receptionist.md) |
| 预处理器链        | `PreHandlerChain`, `AuthenticationDelegate` | [预处理器](docs/5.0.3/prehandler.md) |
| 统一响应          | `KeelWebResponder`, `KeelWebApiError` | [响应器](docs/5.0.3/responder.md) |
| HTTP 响应萃取     | `KeelWebResponseDynamicExtractor` | [Requester](docs/5.0.3/requester.md) |
| Markdown 文档服务 | `KeelFastDocsKit` | [FastDocs](docs/5.0.3/fastdocs.md) |
| TCP               | `KeelAbstractSocketWrapper`, `KeelBasicSocketWrapper` | [TCP](docs/5.0.3/tcp.md) |
| UDP               | `KeelUDPTransceiver` | [UDP](docs/5.0.3/udp.md) |

完整的 5.0.3 使用文档见 [docs/5.0.3](docs/5.0.3/index.md)，在线文档入口见 [docs/index.md](docs/index.md)。

## 5.0.3 注意事项

- FastDocs 支持同一服务安装多个实例，目录缓存按实例隔离；Markdown 图片和附件从相同的文档根目录提供。
- `AbstractRequestBody` 依赖 Vert.x `BodyHandler`。它读取 JSON 和普通表单字段；multipart 文件请通过 `RoutingContext.fileUploads()` 访问。
- TCP/UDP 的 INFO 日志只记录报文摘要，不记录 payload。排障时可用 `setDebugPayloadLoggingEnabled(true)` 开启 DEBUG payload，并用 `setDebugPayloadMaxBytes(...)` 限制采样长度。

## 构建与验证

```shell
./gradlew clean test javadoc
```

生成的 Javadoc 位于 `build/docs/javadoc`。GitHub Pages 文档源文件位于 `docs`。

## 许可证

本项目使用 [GNU General Public License v3.0](LICENSE)。
