---
title: 预处理器 - PreHandler
---

# 预处理器 - PreHandler

预处理器链（PreHandlerChain）负责在请求到达业务逻辑之前进行一系列预处理操作，包括平台级处理、安全策略、认证、授权等。

## 预处理器执行顺序

按照 Vert.x Web Handler 的权重机制，预处理器按以下顺序执行：

```
1. PLATFORM         → KeelPlatformHandler + TimeoutHandler + ResponseTimeHandler + 自定义 PlatformHandler
2. SECURITY_POLICY  → CorsHandler 等安全策略处理器
3. PROTOCOL_UPGRADE → 协议升级处理器
4. BODY             → BodyHandler（按需，由 @ApiMeta.requestBodyNeeded 控制）
5. MULTI_TENANT     → 多租户处理器
6. AUTHENTICATION   → 认证处理器（识别用户身份）
7. INPUT_TRUST      → 输入信任处理器
8. AUTHORIZATION    → 授权处理器（确认用户权限）
9. USER             → 自定义用户处理器
```

## KeelPlatformHandler

内置的平台级处理器，自动为每个请求：

- 分配唯一请求 ID（格式：`{本机IP}-{递增ID}-{UUID}`），存入 `RoutingContext` 的 `KEEL_REQUEST_ID` 键
- 记录请求到达时间（毫秒），存入 `RoutingContext` 的 `KEEL_REQUEST_START_TIME` 键

请求 ID 可通过 `KeelWebReceptionist.readRequestID()` 获取。

## PreHandlerChain

预处理器链的基础实现类。通过子类化并在构造方法中向各处理器列表添加元素来实现自定义预处理逻辑。

### 可扩展的处理器列表

| 字段                        | 类型                              | 说明                        |
|---------------------------|---------------------------------|---------------------------|
| `platformHandlers`        | `List<PlatformHandler>`         | 额外的平台级处理器                 |
| `securityPolicyHandlers`  | `List<SecurityPolicyHandler>`   | 安全策略（如 CORS）              |
| `protocolUpgradeHandlers` | `List<ProtocolUpgradeHandler>`  | 协议升级处理器                   |
| `multiTenantHandlers`     | `List<MultiTenantHandler>`      | 多租户处理器                    |
| `authenticationHandlers`  | `List<AuthenticationHandler>`   | 认证处理器                     |
| `inputTrustHandlers`      | `List<InputTrustHandler>`       | 输入信任处理器                   |
| `authorizationHandlers`   | `List<AuthorizationHandler>`    | 授权处理器                     |
| `userHandlers`            | `List<Handler<RoutingContext>>` | 自定义用户处理器                  |
| `uploadDirectory`         | `String`                        | 文件上传目录（默认 `file-uploads`） |
| `failureHandler`          | `Handler<RoutingContext>`       | 失败处理器                     |

### 创建自定义预处理器链

```java
public class MyPreHandlerChain extends PreHandlerChain {
    public MyPreHandlerChain() {
        // 添加 CORS 支持
        securityPolicyHandlers.add(
            CorsHandler.create()
                .addOrigin("https://example.com")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedHeader("Authorization")
        );

        // 添加认证处理器
        authenticationHandlers.add(
            buildAuthenticationHandlerWithDelegate(new MyAuthDelegate())
        );
    }
}
```

## @PreHandlerChainMeta

通过注解将预处理器链绑定到请求接待类。注解查找会沿类继承层级向上搜索，直到找到 `KeelWebReceptionist`。

```java
@PreHandlerChainMeta(MyPreHandlerChain.class)
@ApiMeta(routePath = "/api/protected", allowMethods = {"POST"})
public class ProtectedReceptionist extends KeelWebFutureReceptionist<JsonObject> {
    // ...
}
```

### 继承层级应用

可以在基类上标注 `@PreHandlerChainMeta`，所有子类将自动继承该预处理器链：

```java
@PreHandlerChainMeta(MyPreHandlerChain.class)
public abstract class BaseProtectedReceptionist<R> extends KeelWebFutureReceptionist<R> {
    // ...
}

@ApiMeta(routePath = "/api/users")
public class UsersReceptionist extends BaseProtectedReceptionist<JsonObject> {
    // 自动应用 MyPreHandlerChain
}
```

## AuthenticationDelegate

用于构建认证处理器的代理接口。实现 `authenticate` 方法来解析请求并返回已认证的用户。

```java
public class MyAuthDelegate implements AuthenticationDelegate {
    @Override
    public Future<User> authenticate(RoutingContext routingContext) {
        String token = routingContext.request().getHeader("Authorization");
        if (token == null) {
            return Future.failedFuture("Missing token");
        }
        return validateToken(token)
            .map(userInfo -> User.fromName(userInfo.getName()));
    }
}
```

### 在 PreHandlerChain 中使用

有两种方式创建 `AuthenticationHandler`：

```java
// 方式一：通过 AuthenticationDelegate 实例
AuthenticationHandler handler = AuthenticationDelegate.build(new MyAuthDelegate());

// 方式二：通过 Lambda 函数
AuthenticationHandler handler = AuthenticationDelegate.build(ctx -> {
    // 认证逻辑
    return Future.succeededFuture(User.fromName("anonymous"));
});
```

也可使用 `PreHandlerChain` 提供的便捷方法：

```java
AuthenticationHandler handler = buildAuthenticationHandlerWithDelegate(new MyAuthDelegate());
```
