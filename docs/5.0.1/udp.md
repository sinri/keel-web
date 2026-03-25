---
title: UDP 通信
---

# UDP 通信

Keel-Web 提供了 `KeelUDPTransceiver`，封装 Vert.x `DatagramSocket`，用于 UDP 数据报的收发。

## KeelUDPTransceiver

实现了 `Closeable` 接口的 UDP 收发器。

### 创建实例

```java
DatagramSocket udpSocket = vertx.createDatagramSocket();
KeelUDPTransceiver transceiver = new KeelUDPTransceiver(
    udpSocket,
    8888,         // 监听端口
    "0.0.0.0"    // 监听地址
);
```

### 设置数据接收回调

```java
transceiver.setDatagramSocketConsumer((sender, buffer) -> {
    String senderHost = sender.hostAddress();
    int senderPort = sender.port();
    String message = buffer.toString();
    System.out.println("From " + senderHost + ":" + senderPort + " -> " + message);
});
```

### 开始监听

```java
transceiver.listen()
    .onSuccess(v -> System.out.println("UDP listening on port 8888"))
    .onFailure(Throwable::printStackTrace);
```

### 发送数据

```java
Buffer data = Buffer.buffer("Hello UDP");
transceiver.send(data, 9999, "192.168.1.100")
    .onSuccess(v -> System.out.println("Sent"))
    .onFailure(Throwable::printStackTrace);
```

### 关闭

```java
transceiver.close()
    .onSuccess(v -> System.out.println("UDP transceiver closed"));
```

也可通过 Vert.x 的 `Closeable` 机制自动关闭。

## 完整示例

### UDP 回声服务

```java
DatagramSocket socket = vertx.createDatagramSocket();
KeelUDPTransceiver transceiver = new KeelUDPTransceiver(socket, 8888, "0.0.0.0");

transceiver.setDatagramSocketConsumer((sender, buffer) -> {
    // 将收到的数据原样发回
    transceiver.send(buffer, sender.port(), sender.hostAddress());
});

transceiver.listen()
    .onSuccess(v -> System.out.println("Echo server started"));
```

### UDP 日志收集器

```java
DatagramSocket socket = vertx.createDatagramSocket();
KeelUDPTransceiver receiver = new KeelUDPTransceiver(socket, 5140, "0.0.0.0");

receiver.setDatagramSocketConsumer((sender, buffer) -> {
    String logLine = buffer.toString();
    processLogLine(sender.hostAddress(), logLine);
});

receiver.listen();
```

## 日志

`KeelUDPTransceiver` 自带日志记录器，主题为 `UdpDatagram`，自动记录：

- 数据报接收事件（含发送方地址和端口）
- 数据报发送事件（含目标地址和端口）
- 异常事件
- 关闭事件

通过 `getLogger()` 可获取日志记录器进行自定义日志输出。
