# keel-web 项目审查记录（2026-07-10）

## 项目概况

- 仓库：`https://github.com/sinri/keel-web.git`
- 类型：Java 17 模块化 Web 工具库
- 主要依赖：Vert.x Web / Web Client、keel-core、jspecify
- 初次验证：
  - `./gradlew compileJava`：通过
  - `./gradlew test`：通过，但当时 `test NO-SOURCE`，没有 Java 测试被执行
  - `./gradlew javadoc`：通过
- 当前分支复核（提交 `8e9aaa3`）：
  - `./gradlew clean test javadoc`：通过
  - 共执行 9 个测试，0 skipped、0 failures、0 errors

## 工作区状态

审查开始前已有未提交改动：

- `gradle.properties`：已修改
- `docs/Gemfile`：未跟踪

本审查未修改源码和上述文件。

## 问题清单

### PR-001 FastDocs 目录页缓存是全局静态值，会串用不同文档实例

- 严重级别：中
- 状态：已修复并增加回归测试
- GitHub Issue：`https://github.com/sinri/keel-web/issues/3`
- 位置：`src/main/java/io/github/sinri/keel/web/http/fastdocs/page/CataloguePageBuilder.java:26`, `:158`
- 现象：`catalogueDivContentCache` 是单个 `static volatile String`，第一次构建目录后，后续所有 `CataloguePageBuilder` 实例都会复用同一份 HTML。
- 风险：如果一个服务安装多个 FastDocs 根路径，或者运行时文档目录/标题不同，目录页会显示第一次请求对应的目录和链接，造成页面错乱。
- 建议：把缓存改为按 `rootURLPath + rootMarkdownFilePath + subject` 分键，或移到 `KeelFastDocsKit` 实例级别；如果目录内容可能动态变化，则提供禁用缓存或刷新入口。
- 可选处理：快速修复 / 建 GitHub Issue / 不处理
- 处理结果：提交 `b613c16` 将缓存隔离到 `KeelFastDocsKit` 实例，并分别缓存独立目录与文档内目录；提交 `8e9aaa3` 增加回归覆盖。

### PR-002 FastDocs 静态资源处理没有使用文档根目录

- 严重级别：中
- 状态：已修复并增加回归测试
- GitHub Issue：`https://github.com/sinri/keel-web/issues/4`
- 位置：`src/main/java/io/github/sinri/keel/web/http/fastdocs/KeelFastDocsKit.java:39`, `:178`
- 现象：构造函数创建 `StaticHandler.create()`，静态请求最终直接调用 `staticHandler.handle(options.ctx)`，但没有把 `rootMarkdownFilePath` 配到 StaticHandler 的 web root。
- 风险：Markdown 文件从 `rootMarkdownFilePath` 读取，而图片、附件等非 `.md` 静态资源会按 Vert.x 默认 `webroot` 查找。文档目录中的图片/附件很可能 404，FastDocs 的资源模型不一致。
- 建议：创建 `StaticHandler.create(rootMarkdownFilePath)`，并确认 URL 相对路径和 `rootURLPath` 的关系；同时增加一个含图片资源的 FastDocs 测试。
- 可选处理：快速修复 / 建 GitHub Issue / 不处理
- 处理结果：提交 `45ff55d` 将静态资源根目录绑定到 Markdown 文档根目录；提交 `8e9aaa3` 增加含 SVG 资源的回归覆盖。

### PR-003 Gradle 构建脚本依赖本地私有属性，干净环境可能无法配置项目

- 严重级别：中
- 状态：不处理
- 位置：`build.gradle.kts:23`, `:36`, `:38`, `:39`, `:148`, `:154`, `:217`, `:218`
- 现象：构建配置直接声明 `sonatypeUsername` / `sonatypePassword`，并把 `internalNexus*` 属性强制转换为 `String`。这些值没有在仓库内的 `gradle.properties` 提供。
- 风险：本机能通过，是因为环境中可能有用户级 Gradle 属性；但开源仓库、CI 或新贡献者环境可能在配置阶段失败，甚至只是执行 `tasks` 或 `compileJava` 也需要这些私有属性。
- 建议：依赖解析默认只使用 Maven Central；内部 Nexus 仓库仅在 URL 存在时注册。发布凭据改为 `providers.gradleProperty(...).orElse(providers.environmentVariable(...))`，并只在发布任务需要时读取。
- 可选处理：快速修复 / 建 GitHub Issue / 不处理

### PR-004 当前没有 Java 测试，`test` 任务不会验证核心行为

- 严重级别：中
- 状态：已处理
- GitHub Issue：`https://github.com/sinri/keel-web/issues/5`
- 位置：`build.gradle.kts:89`, `src/test`
- 现象：`src/test` 只有 `resources/config.properties`，没有 Java 测试类；`./gradlew test` 输出 `NO-SOURCE`。
- 风险：路由装载、预处理链、响应格式、FastDocs 路径安全、请求提取器等公开 API 没有回归保护。库升级 Vert.x 或 keel-core 时，行为回归很难被 CI 提前发现。
- 建议：先补最小回归测试：FastDocs 目录缓存/静态资源、`KeelWebResponseDynamicExtractor` 状态码处理、`KeelWebReceptionistLoader` 路由注册、`AbstractRequestBody` JSON/form 解析。
- 可选处理：建 GitHub Issue / 不处理
- 处理结果：提交 `09a06df`、`8e9aaa3` 已增加 9 个核心回归测试，当前 `test` 任务不再是 `NO-SOURCE`。

### PR-005 表单请求体解析对 Content-Type 和 multipart 的处理语义不清

- 严重级别：低
- 状态：已修复并增加回归测试
- GitHub Issue：`https://github.com/sinri/keel-web/issues/6`
- 位置：`src/main/java/io/github/sinri/keel/web/http/receptionist/AbstractRequestBody.java:26`, `:28`, `:31`, `src/main/java/io/github/sinri/keel/web/http/prehandler/PreHandlerChain.java:80`
- 现象：`AbstractRequestBody` 手动检查 `Content-Type` 字符串，且大小写敏感；遇到 `multipart/form-data` 时在解析阶段调用 `setExpectMultipart(true)`，但默认预处理链中 `BodyHandler` 已经在更早阶段处理了请求体。
- 风险：代码注释说支持 JSON 和 FORM（包括分块支持），但 multipart 文件上传、大小写不同的 content type、带复杂参数的 content type 的行为不够明确。后续调用方可能误以为这里能完整抽取 multipart 内容。
- 建议：明确 `AbstractRequestBody` 只抽取 JSON 和 form attributes，文件上传通过 `RoutingContext.fileUploads()` 读取；Content-Type 判断改为大小写无关，并去掉或前移 `setExpectMultipart(true)`。
- 可选处理：快速修复 / 建 GitHub Issue / 不处理
- 处理结果：提交 `7259486` 改用 Vert.x 解析后的 MIME 类型，明确 multipart 文件读取契约，并移除过晚的 multipart 配置；提交 `8e9aaa3` 增加回归覆盖。

### PR-006 TCP/UDP 默认在 INFO 日志中写入完整报文负载

- 严重级别：中
- 状态：已快速修复，待用户确认提交
- 位置：`src/main/java/io/github/sinri/keel/web/tcp/KeelAbstractSocketWrapper.java:51`, `src/main/java/io/github/sinri/keel/web/tcp/SocketSpecificLog.java:23`, `src/main/java/io/github/sinri/keel/web/udp/KeelUDPTransceiver.java:60`, `:74`, `src/main/java/io/github/sinri/keel/web/udp/DatagramSpecificLog.java:24`
- 现象：TCP 每次收包、UDP 每次收发包都在 INFO 级别调用日志对象的 `buffer(...)`，把整个 Buffer 十六进制编码后写入 `buffer_content`。
- 风险：业务报文中的凭据、令牌、个人信息或协议明文会进入常规生产日志；大包和高吞吐场景还会产生约两倍大小的十六进制字符串、对象分配和日志存储开销，可能放大延迟与日志成本。
- 建议：INFO 默认仅记录方向、对端地址和字节数；完整 payload 降到显式启用的 DEBUG/TRACE，且提供最大采样长度与脱敏扩展点。即使关闭 INFO 输出，也应确认日志 supplier 不会提前执行十六进制编码。
- 可选处理：快速修复 / 建 GitHub Issue / 不处理
- 处理结果：INFO 日志现仅记录报文大小及 UDP 端点信息；TCP/UDP 均新增显式 DEBUG payload 开关，默认关闭，开启后默认最多采样 256 字节，并支持调整采样上限。保留原公开日志方法签名并将其改为安全摘要语义，避免源码兼容性破坏。

## 后续决策记录

- PR-001：用户决策为建立 GitHub Issue，已创建 `https://github.com/sinri/keel-web/issues/3`。
- PR-002：用户决策为建立 GitHub Issue，已创建 `https://github.com/sinri/keel-web/issues/4`。
- PR-003：用户决策为不处理。
- PR-004：用户决策为建立 GitHub Issue，已创建 `https://github.com/sinri/keel-web/issues/5`。
- PR-005：用户决策为建立 GitHub Issue，已创建 `https://github.com/sinri/keel-web/issues/6`。
- PR-001、PR-002、PR-004、PR-005：当前分支已完成修复或测试补充，并通过复核。
- PR-006：用户决策为快速修复；代码与完整质量检查已完成，待用户确认是否提交。
