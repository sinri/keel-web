---
title: TCP 通信
---

# TCP 通信

Keel-Web 提供了对 Vert.x `NetSocket` 的封装，以 Verticle 形式管理 TCP Socket 连接的生命周期。

## 类层次

```
KeelAbstractSocketWrapper extends KeelVerticleBase    (抽象基类)
└── KeelBasicSocketWrapper                            (动态回调实现)
```

## KeelAbstractSocketWrapper

TCP Socket 的抽象封装类，作为 Verticle 部署，内置 Funnel（漏斗）机制来有序处理收到的数据。

### 创建子类

```java
public class MySocketHandler extends KeelAbstractSocketWrapper {
    public MySocketHandler(NetSocket socket) {
        super(socket);
    }

    // 或者指定自定义 socketID
    public MySocketHandler(NetSocket socket, String socketID) {
        super(socket, socketID);
    }

    @Override
    protected Future<Void> whenBufferComes(Buffer incomingBuffer) {
        // 处理收到的数据
        String message = incomingBuffer.toString();
        return write("Echo: " + message);
    }
}
```

### 生命周期回调

| 方法                                 | 说明           | 是否必须实现  |
|------------------------------------|--------------|---------|
| `whenBufferComes(Buffer)`          | 收到数据时触发      | 是（抽象方法） |
| `whenReadToEnd()`                  | 对端关闭写入时触发    | 否       |
| `whenDrain()`                      | 写队列从满到可写时触发  | 否       |
| `whenClose()`                      | Socket 关闭时触发 | 否       |
| `whenExceptionOccurred(Throwable)` | 发生异常时触发      | 否       |

### 写入数据

```java
// 写入字符串
write("Hello").onSuccess(v -> { ... });

// 写入指定编码的字符串
write("你好", "UTF-8");

// 写入 Buffer
write(Buffer.buffer(bytes));
```

写入时自动处理背压：若写队列已满，Socket 会暂停接收数据（`pause`），待队列有空间后自动恢复（`resume`）。

### 其他功能

```java
// 获取 Socket ID
String id = getSocketID();

// 获取远程/本地地址
SocketAddress remote = getRemoteAddress();
String remoteStr = getRemoteAddressString(); // "host:port"
SocketAddress local = getLocalAddress();
String localStr = getLocalAddressString();

// 设置写队列最大容量
setMaxSize(1024 * 1024);

// 关闭连接
close();
```

### 日志

每个 Socket 实例自带日志记录器，主题为 `TcpSocket`，分类标签为 `socket_id:{socketID}`。

```java
getLogger().info(r -> r.message("Custom log").buffer(someBuffer));
```

## KeelBasicSocketWrapper

`KeelAbstractSocketWrapper` 的动态回调实现，通过设置函数式回调来处理各事件，适用于不需要创建子类的场景。

### 用法

```java
NetSocket socket = ...;
KeelBasicSocketWrapper wrapper = new KeelBasicSocketWrapper(socket)
    .setIncomingBufferProcessor(buffer -> {
        // 处理收到的数据
        return Future.succeededFuture();
    })
    .setReadToEndHandler(v -> {
        System.out.println("Read to end");
    })
    .setDrainHandler(v -> {
        System.out.println("Writable again");
    })
    .setCloseHandler(v -> {
        System.out.println("Socket closed");
    })
    .setExceptionHandler(throwable -> {
        throwable.printStackTrace();
    });

// 部署为 Verticle
wrapper.deployMe(keel);
```

所有回调设置方法必须在 `deployMe` 之前调用。

## 在 TCP 服务端中使用

结合 Vert.x `NetServer` 使用：

```java
NetServer netServer = vertx.createNetServer();
netServer.connectHandler(socket -> {
    KeelBasicSocketWrapper wrapper = new KeelBasicSocketWrapper(socket)
        .setIncomingBufferProcessor(buffer -> {
            return wrapper.write("Received: " + buffer.toString());
        })
        .setCloseHandler(v -> {
            System.out.println("Client disconnected");
        });
    wrapper.deployMe(keel);
});
netServer.listen(8888);
```
