package io.github.sinri.keel.web.http.fastdocs;

import io.github.sinri.keel.core.utils.FileUtils;
import io.github.sinri.keel.web.http.fastdocs.page.CataloguePageBuilder;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class CatalogueResourcePathUnitTest {
    @TempDir
    Path tempDir;

    @Test
    void fileResourceRootsAreDecodedAsUris() throws Exception {
        for (String parent : List.of("plain", "with spaces", "中文目录", "plus+percent%20")) {
            Path deployment = Files.createDirectories(tempDir.resolve(parent));
            Path docs = Files.createDirectories(deployment.resolve("isolated-docs"));
            Files.writeString(docs.resolve("index.md"), "# Index");
            Files.writeString(docs.resolve("C++.md"), "# C++");
            Path child = Files.createDirectories(docs.resolve("子 +目录"));
            Files.writeString(child.resolve("index.md"), "# Nested index");
            Files.writeString(child.resolve("visible.md"), "# Visible");
            assertTree(readTree(deployment.toUri().toURL()));
        }
    }

    @Test
    void jarResourcesKeepTheirIndependentTraversalAndEncodedLinks() throws Exception {
        Path jar = tempDir.resolve("文档 with spaces.jar");
        try (var out = new JarOutputStream(Files.newOutputStream(jar))) {
            for (String entry : List.of("isolated-docs/", "isolated-docs/index.md", "isolated-docs/C++.md",
                    "isolated-docs/子 +目录/", "isolated-docs/子 +目录/index.md", "isolated-docs/子 +目录/visible.md")) {
                out.putNextEntry(new JarEntry(entry));
                if (!entry.endsWith("/")) out.write("# Document".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        assertTree(readTree(jar.toUri().toURL()));
    }

    private static void assertTree(JsonObject tree) {
        assertEquals("/docs/index.md", tree.getString("href"));
        var children = tree.getJsonArray("children");
        assertEquals(2, children.size());
        assertEquals("C++.md", children.getJsonObject(0).getString("name"));
        assertEquals("/docs/C%2B%2B.md", children.getJsonObject(0).getString("href"));
        var nested = children.getJsonObject(1);
        assertEquals("子 +目录", nested.getString("name"));
        String prefix = "/docs/%E5%AD%90%20%2B%E7%9B%AE%E5%BD%95/";
        assertEquals(prefix + "index.md", nested.getString("href"));
        assertEquals(1, nested.getJsonArray("children").size());
        assertEquals(prefix + "visible.md", nested.getJsonArray("children").getJsonObject(0).getString("href"));
    }

    private static JsonObject readTree(URL resourceRoot) throws Exception {
        // Isolate the defining classloaders used by both the builder and the JAR utility.
        URL[] urls = {resourceRoot,
                CataloguePageBuilder.class.getProtectionDomain().getCodeSource().getLocation(),
                FileUtils.class.getProtectionDomain().getCodeSource().getLocation()};
        try (var loader = new URLClassLoader(urls, CatalogueResourcePathUnitTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("io.github.sinri.keel.web.http.fastdocs.")
                        || name.equals(FileUtils.class.getName())) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null) loaded = findClass(name);
                        if (resolve) resolveClass(loaded);
                        return loaded;
                    }
                }
                return super.loadClass(name, resolve);
            }
        }) {
            Class<?> optionsClass = loader.loadClass(PageBuilderOptions.class.getName());
            Object options = optionsClass.getConstructor().newInstance();
            optionsClass.getField("rootMarkdownFilePath").set(options, "isolated-docs/");
            optionsClass.getField("rootURLPath").set(options, "/docs/");
            optionsClass.getField("subjectOfDocuments").set(options, "Isolated docs");
            Class<?> builderClass = loader.loadClass(CataloguePageBuilder.class.getName());
            Object builder = builderClass.getConstructor(optionsClass).newInstance(options);
            Object tree = builderClass.getMethod("buildCatalogueTree").invoke(builder);
            return (JsonObject) tree.getClass().getMethod("toJsonObject").invoke(tree);
        }
    }
}
