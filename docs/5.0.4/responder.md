---
title: 5.0.4 响应器失败响应变更
---

# 5.0.4 响应器失败响应变更

标准 JSON 响应器 `KeelWebResponder.createCommonInstance` 的失败响应仅公开异常类型和安全消息。
成功响应及 HTTP 状态码规则保持不变，其余接口用法参见 [响应器文档](../5.0.3/responder.md)。

## 内部异常

`KeelWebApiError.wrap` 将普通异常包装为 HTTP 500，公开消息固定为 `Internal server error`，原始异常保留为 cause 供服务端诊断。

```json
{
  "request_id": "abc-123",
  "code": "FAILED",
  "data": null,
  "throwable": {
    "class": "io.github.sinri.keel.web.http.receptionist.responder.KeelWebApiError",
    "message": "Internal server error"
  }
}
```

`throwable.class` 为传入的 `KeelWebApiError` 实例的实际全限定类名（包含业务子类），不是 cause 的类型。
`throwable` 不再包含堆栈、cause、suppressed 或其他异常属性；此行为不受 DEBUG 日志开关影响。

## 业务异常与额外数据

显式构造 `KeelWebApiError` 时，消息视为可公开的业务消息。调用方应确保消息、业务子类名称及 extra 适合公开，不应将底层异常消息直接拼入业务消息或 extra。
对已有 `KeelWebApiError` 调用 `wrap` 会保留原实例。

例如，HTTP 422 的业务异常附带字段提示：

```json
{
  "request_id": "abc-123",
  "code": "FAILED",
  "data": {
    "extra": {
      "field": "email"
    }
  },
  "throwable": {
    "class": "io.github.sinri.keel.web.http.receptionist.responder.KeelWebApiError",
    "message": "Invalid parameters"
  }
}
```

未传 `ValueBox` 时，`data` 为 `null`。传入的盒子未设置值、值为 null 或额外数据无法序列化时，`data` 为：

```json
{
  "extra_render_error": "Unable to render extra data"
}
```

## 日志与兼容性

响应器以 ERROR 级别记录完整异常及 `request_id`；extra 处理失败时另记一条 ERROR 日志。
诊断日志不依赖 DEBUG，但仍受日志器的最低可见级别和输出配置控制。

已有客户端可以继续读取 `throwable.class` 和 `throwable.message`。
依赖堆栈、cause 或无 extra 时的 `data.extra_render_error` 的客户端需要调整。
内部错误的详细消息请通过响应中的 `request_id` 查询服务端日志。
