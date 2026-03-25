---
title: FastDocs - Markdown 文档系统
---

# FastDocs - Markdown 文档系统

FastDocs 提供一个开箱即用的、基于 Markdown 文件系统的文档服务，可将存放在
`resources` 目录中的 Markdown 文件渲染为 HTML 页面，并通过 HTTP 路由提供访问。

## 核心类

| 类                    | 说明                              |
|----------------------|---------------------------------|
| `KeelFastDocsKit`    | FastDocs 主类，负责路由处理和 Markdown 渲染 |
| `PageBuilderOptions` | 页面构建参数                          |

## 快速集成

### 一行安装

```java
@Override
private void configureRoutes(Router router) {
    KeelFastDocsKit.installToRouter(
        router,
        "/docs/",                // URL 前缀
        "webroot/markdown/",     // Markdown 文件在 resources 中的路径
        "My Project Docs",       // 文档标题
        "© 2025 My Company"      // 页脚文本
    );
}
```

### 手动创建

如需更精细的控制：

```java
KeelFastDocsKit kit = new KeelFastDocsKit("/docs/", "webroot/markdown/")
    .setDocumentSubject("API Documentation")
    .setFooterText("Internal Use Only");

router.route("/docs/*").handler(kit::processRouterRequest);
```

## 文件结构

将 Markdown 文件放置在 `src/main/resources` 下的指定目录中：

```
src/main/resources/
└── webroot/
    └── markdown/
        ├── index.md          ← 首页（默认跳转）
        ├── getting-started.md
        ├── api/
        │   ├── overview.md
        │   └── reference.md
        └── images/
            └── logo.png      ← 静态资源
```

## URL 路由规则

假设 URL 前缀为 `/docs/`：

| 请求路径                    | 处理方式                    |
|-------------------------|-------------------------|
| `/docs/` 或 `/docs`      | 重定向到 `/docs/index.md`   |
| `/docs/xxx.md`          | 渲染 Markdown 文件为 HTML 页面 |
| `/docs/catalogue`       | 显示文档目录页                 |
| `/docs/markdown.css`    | 返回 Markdown 样式表         |
| `/docs/images/logo.png` | 作为静态资源返回                |

## 功能特性

### Markdown 渲染

- 自动将 `.md` 文件渲染为带样式的 HTML 页面
- 内置 Markdown CSS 样式
- 包含文档标题和页脚

### 目录导航

访问 `/docs/catalogue` 可查看文档目录，支持 `from_doc` 参数标记当前文档位置：

```
/docs/catalogue?from_doc=getting-started.md
```

### 静态资源

非 `.md` 后缀的文件请求将通过 Vert.x `StaticHandler` 直接返回，可用于图片、CSS、JS 等静态资源的访问。

### 安全

- 仅支持 GET 请求，其他方法返回 405
- 内置路径遍历防护，会对请求路径进行规范化和校验
- 无效的 Markdown 文件路径返回 404

## PageBuilderOptions

页面构建参数类，包含以下字段：

| 字段                     | 类型             | 说明                             |
|------------------------|----------------|--------------------------------|
| `rootURLPath`          | String         | URL 根路径                        |
| `rootMarkdownFilePath` | String         | Markdown 文件根路径                 |
| `ctx`                  | RoutingContext | 路由上下文                          |
| `markdownContent`      | String         | 当前 Markdown 文件内容               |
| `subjectOfDocuments`   | String         | 文档标题（默认 `"FastDocs"`）          |
| `footerText`           | String         | 页脚文本（默认 `"Without Copyright"`） |
| `fromDoc`              | String         | 来源文档路径（目录页使用）                  |
