---
title: HTTP 客户端 - Requester
---

# HTTP 客户端 - Requester

Keel-Web 提供了一套 HTTP 响应萃取器（Extractor）和错误类型体系，配合 Vert.x `WebClient` 使用，简化对 HTTP 响应的解析和验证。

## 响应萃取器

### 类层次

```
KeelWebResponseExtractor<T>                     (抽象基类)
├── KeelWebResponseExtractorOnNormalStatus       (验证 200 状态码，萃取 Buffer)
├── KeelWebResponseExtractorOnJsonObjectFormat   (验证 200 + JSON 格式，萃取 JsonObject)
│   └── KeelWebResponseExtractorOnOKCode         (验证 200 + JSON + code=OK)

KeelWebResponseDynamicExtractor<T>               (动态萃取器，独立体系)
```

### KeelWebResponseExtractor\<T\>

所有萃取器的基类，接收 HTTP 响应并尝试萃取目标类型。

```java
KeelWebResponseExtractorOnOKCode extractor = new KeelWebResponseExtractorOnOKCode(
    "GetUserAPI",     // 请求标签，用于日志
    httpResponse      // Vert.x HttpResponse<Buffer>
);
JsonObject result = extractor.extract();
```

### KeelWebResponseExtractorOnNormalStatus

验证 HTTP 状态码为 200，萃取原始 `Buffer`。

```java
Buffer body = new KeelWebResponseExtractorOnNormalStatus("DownloadFile", response).extract();
```

如状态码非 200，抛出 `ReceivedAbnormalStatusResponse`。

### KeelWebResponseExtractorOnJsonObjectFormat

验证 HTTP 状态码为 200 且响应体为合法 JSON，萃取 `JsonObject`。

```java
JsonObject json = new KeelWebResponseExtractorOnJsonObjectFormat("QueryAPI", response).extract();
```

如状态码非 200，抛出 `ReceivedAbnormalStatusResponse`；如响应体不是合法 JSON，抛出 `ReceivedUnexpectedFormatResponse`。

### KeelWebResponseExtractorOnOKCode

在 `KeelWebResponseExtractorOnJsonObjectFormat` 基础上，进一步验证 JSON 中的 `code` 字段为 `"OK"`。

```java
JsonObject json = new KeelWebResponseExtractorOnOKCode("CreateOrder", response).extract();
JsonObject data = json.getJsonObject("data");
```

如 `code` 字段不为 `"OK"`，抛出 `ReceivedFailedResponse`。

### KeelWebResponseDynamicExtractor\<T\>

动态萃取器，支持自定义状态码验证和响应体转换逻辑。

```java
// 基本用法：期望 200 状态码，将响应体转为 JsonObject
KeelWebResponseDynamicExtractor<JsonObject> extractor =
    new KeelWebResponseDynamicExtractor<>(buffer -> buffer.toJsonObject());
JsonObject result = extractor.extract(response);

// 带请求标签
KeelWebResponseDynamicExtractor<String> extractor =
    new KeelWebResponseDynamicExtractor<>("FetchHTML", buffer -> buffer.toString());
String html = extractor.extract(response);

// 自定义期望状态码集合
KeelWebResponseDynamicExtractor<JsonObject> extractor =
    new KeelWebResponseDynamicExtractor<>(
        "MultiStatusAPI",
        buffer -> buffer.toJsonObject(),
        Set.of(200, 201, 204)
    );
```

## 错误类型

### 类层次

```
ReceivedUnexpectedResponse              (基类)
├── ReceivedAbnormalStatusResponse      (HTTP 状态码不符合预期)
├── ReceivedFailedResponse              (业务 code 不为 OK)
└── ReceivedUnexpectedFormatResponse    (响应体格式不符合预期)
```

### ReceivedUnexpectedResponse

所有响应错误的基类，携带请求标签、HTTP 状态码和原始响应体。

```java
try {
    JsonObject result = extractor.extract();
} catch (ReceivedUnexpectedResponse e) {
    int statusCode = e.getResponseStatusCode();
    Buffer body = e.getResponseBody();
    JsonObject errorInfo = e.toJsonObject();
}
```

`toJsonObject()` 输出示例：

```json
{
    "error": "io.github.sinri.keel.web.http.requester.error.ReceivedAbnormalStatusResponse",
    "message": "{GetUserAPI} Abnormal status code",
    "response": {
        "status_code": 500,
        "body": "..."
    }
}
```

### 错误类型汇总

| 错误类型                               | 触发条件                        |
|------------------------------------|-----------------------------|
| `ReceivedAbnormalStatusResponse`   | HTTP 状态码非预期值（通常非 200）       |
| `ReceivedFailedResponse`           | JSON 响应中 `code` 字段不为 `"OK"` |
| `ReceivedUnexpectedFormatResponse` | 响应体无法按预期格式解析                |

## 配合 Vert.x WebClient 的典型用法

```java
WebClient client = WebClient.create(vertx);

client.getAbs("https://api.example.com/users/1")
    .send()
    .map(response -> new KeelWebResponseExtractorOnOKCode("GetUser", response).extract())
    .map(json -> json.getJsonObject("data"))
    .onSuccess(userData -> {
        // 处理用户数据
    })
    .onFailure(error -> {
        if (error instanceof ReceivedUnexpectedResponse e) {
            logger.error("API Error: " + e.toJsonObject().encode());
        }
    });
```
