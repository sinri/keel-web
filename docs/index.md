---
title: Keel-Web Documentation
---

# Keel-Web Documentation

Keel-Web 是 [Keel](https://github.com/sinri/keel-web) 框架的 Web 功能组件，基于 [Vert.x 5](https://vertx.io/) 构建，提供 HTTP 服务端、HTTP 客户端、TCP 和 UDP 网络通信等能力。

## 版本文档

| 版本  | 状态         | 链接                 |
|-------|--------------|----------------------|
| 5.0.3 | 当前稳定版本 | [查看文档](./5.0.3/) |
| 5.0.1 | 历史版本     | [查看文档](./5.0.1/) |

## 项目信息

- **源码仓库**: [github.com/sinri/keel-web](https://github.com/sinri/keel-web)
- **许可证**: GPL-v3.0
- **Java 模块**: `io.github.sinri.keel.web`

## Gradle 引入

```groovy
dependencies {
    implementation 'io.github.sinri:keel-web:5.0.3'
}
```

使用 Java 17 或更高版本。5.0.3 基于 Vert.x 5.1.3，并依赖 Keel Core 5.0.3。

## 文档导航

- [5.0.4 响应器失败响应变更](./5.0.4/responder.md)
- [HTTP 服务](./5.0.3/http-server.md)
- [请求接待与请求体](./5.0.3/receptionist.md)
- [预处理器链](./5.0.3/prehandler.md)
- [响应器](./5.0.3/responder.md)
- [HTTP 响应萃取器](./5.0.3/requester.md)
- [FastDocs](./5.0.3/fastdocs.md)
- [TCP 通信](./5.0.3/tcp.md)
- [UDP 通信](./5.0.3/udp.md)
