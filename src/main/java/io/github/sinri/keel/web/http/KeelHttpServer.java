package io.github.sinri.keel.web.http;

import io.github.sinri.keel.base.Keel;
import io.github.sinri.keel.base.verticles.AbstractKeelVerticle;
import io.github.sinri.keel.core.utils.ReflectionUtils;
import io.github.sinri.keel.logger.api.logger.Logger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Keel HTTP 服务基础类。
 *
 * @since 5.0.0
 */
abstract public class KeelHttpServer extends AbstractKeelVerticle {
    @NotNull
    public static final String CONFIG_HTTP_SERVER_PORT = "http_server_port";
    @NotNull
    public static final String CONFIG_HTTP_SERVER_OPTIONS = "http_server_options";
    private static final int DEFAULT_HTTP_SERVER_PORT = 8080;

    @Nullable
    protected HttpServer server;
    private Logger httpServerLogger;

    public KeelHttpServer(@NotNull Keel keel) {
        super(keel);
    }

    protected int getHttpServerPort() {
        JsonObject config = this.config();
        if (config == null) return DEFAULT_HTTP_SERVER_PORT;
        return config.getInteger(CONFIG_HTTP_SERVER_PORT, DEFAULT_HTTP_SERVER_PORT);
    }

    @NotNull
    protected HttpServerOptions getHttpServerOptions() {
        JsonObject config = this.config();
        if (config != null) {
            JsonObject httpServerOptions = config.getJsonObject(CONFIG_HTTP_SERVER_OPTIONS);
            if (httpServerOptions != null) {
                return new HttpServerOptions(httpServerOptions);
            }
        }
        return new HttpServerOptions().setPort(getHttpServerPort());
    }

    protected abstract void configureRoutes(@NotNull Router router);

    /**
     * Executes tasks or setup logic that needs to be completed before the HTTP server starts.
     * This method returns a future that signifies the completion of any preparatory operations
     * required prior to initializing the server.
     *
     * @return a {@link Future} that completes with {@code null} upon successful completion of all pre-server-start
     *         tasks,
     *         or fails with an exception if any errors occur during the process.
     */
    @NotNull
    protected Future<Void> beforeStartServer() {
        return Future.succeededFuture();
    }

    /**
     * Executes tasks or cleanup operations after the HTTP server has shut down.
     * This method is invoked to perform any necessary post-shutdown activities,
     * such as releasing resources or logging the shutdown event.
     *
     * @return a {@link Future} that completes with {@code null} upon the successful
     *         completion of all post-shutdown tasks, or fails with an exception if
     *         an error occurs during the execution of these tasks.
     */
    @NotNull
    protected Future<Void> afterShutdownServer() {
        return Future.succeededFuture();
    }

    @Override
    protected @NotNull Future<Void> startVerticle() {
        this.httpServerLogger = buildHttpServerLogger();

        var server = getVertx().createHttpServer(getHttpServerOptions());
        this.server = server;

        Router router = Router.router(getVertx());
        this.configureRoutes(router);

        return beforeStartServer()
                .compose(v0 -> server
                        .requestHandler(router)
                        .exceptionHandler(throwable -> getHttpServerLogger().error(r -> r.message("KeelHttpServer Exception")
                                                                                         .exception(throwable)))
                        .listen()
                        .compose(httpServer -> {
                            int actualPort = httpServer.actualPort();
                            getHttpServerLogger().info(r -> r.message("HTTP Server Established, Actual Port: " + actualPort));
                            return Future.succeededFuture();
                        }, throwable -> {
                            getHttpServerLogger().error(r -> r.message("Listen failed")
                                                              .exception(throwable));
                            return Future.failedFuture(throwable);
                        }));
    }

    @NotNull
    protected final Logger buildHttpServerLogger() {
        return keel().getLoggerFactory().createLogger("KeelHttpServer");
    }

    public final Logger getHttpServerLogger() {
        return httpServerLogger;
    }

    @Override
    protected @NotNull Future<Void> stopVerticle() {
        if (server == null) return Future.succeededFuture();
        return server.close()
                     .compose(v -> {
                         getHttpServerLogger().info(r -> r.message("HTTP Server Closed"));
                         return afterShutdownServer()
                                 .eventually(Future::succeededFuture);
                     }, throwable -> {
                         getHttpServerLogger().error(r -> r
                                 .message("HTTP Server Closing Failure: %s".formatted(throwable.getMessage()))
                                 .exception(throwable));
                         return Future.failedFuture(throwable);
                     });
    }


    /**
     * Deploys the current verticle with an appropriate threading model configuration.
     * If virtual threads are available, the deployment options will be set to use the virtual threading model.
     *
     * @return a {@link Future} that completes with the deployment ID if the deployment is successful,
     *         or fails with an exception if the deployment fails.
     */
    @NotNull
    public Future<String> deployMe() {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        if (ReflectionUtils.isVirtualThreadsAvailable()) {
            deploymentOptions.setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
        }
        return super.deployMe(deploymentOptions);
    }
}
