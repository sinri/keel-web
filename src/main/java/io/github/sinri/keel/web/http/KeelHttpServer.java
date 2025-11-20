package io.github.sinri.keel.web.http;

import io.github.sinri.keel.base.verticles.AbstractKeelVerticle;
import io.github.sinri.keel.core.utils.ReflectionUtils;
import io.github.sinri.keel.logger.api.factory.LoggerFactory;
import io.github.sinri.keel.logger.api.logger.Logger;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.jetbrains.annotations.NotNull;

import static io.github.sinri.keel.base.KeelInstance.Keel;

/**
 * Keel HTTP 服务基础类。
 *
 * @since 5.0.0
 */
abstract public class KeelHttpServer extends AbstractKeelVerticle implements Closeable {
    public static final String CONFIG_HTTP_SERVER_PORT = "http_server_port";
    public static final String CONFIG_HTTP_SERVER_OPTIONS = "http_server_options";
    private static final int DEFAULT_HTTP_SERVER_PORT = 8080;
    protected HttpServer server;
    private Logger httpServerLogger;

    protected int getHttpServerPort() {
        JsonObject config = this.config();
        if (config == null) return DEFAULT_HTTP_SERVER_PORT;
        return config.getInteger(CONFIG_HTTP_SERVER_PORT, DEFAULT_HTTP_SERVER_PORT);
    }

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

    protected abstract void configureRoutes(Router router);

    /**
     * Executes tasks or setup logic that needs to be completed before the HTTP server starts.
     * This method returns a future that signifies the completion of any preparatory operations
     * required prior to initializing the server.
     *
     * @return a {@link Future} that completes with {@code null} upon successful completion of all pre-server-start
     *         tasks,
     *         or fails with an exception if any errors occur during the process.
     */
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
    protected Future<Void> afterShutdownServer() {
        return Future.succeededFuture();
    }

    @Override
    protected Future<Void> startVerticle() {
        this.httpServerLogger = buildHttpServerLogger();

        this.server = Keel.getVertx().createHttpServer(getHttpServerOptions());

        Router router = Router.router(Keel.getVertx());
        this.configureRoutes(router);

        return beforeStartServer()
                .compose(v0 -> {
                    return server.requestHandler(router)
                                 .exceptionHandler(throwable -> getHttpServerLogger().exception(throwable, r -> r.message("KeelHttpServer Exception")))
                                 .listen()
                                 .compose(httpServer -> {
                                     int actualPort = httpServer.actualPort();
                                     getHttpServerLogger().info(r -> r.message("HTTP Server Established, Actual Port: " + actualPort));
                                     return Future.succeededFuture();
                                 }, throwable -> {
                                     getHttpServerLogger().exception(throwable, r -> r.message("Listen failed"));
                                     return Future.failedFuture(throwable);
                                 });
                });
    }

    /**
     * Override this method to use a customized issue record center.
     *
     */
    public LoggerFactory getLoggerFactory() {
        return Keel.getLoggerFactory();
    }

    @NotNull
    protected final Logger buildHttpServerLogger() {
        return getLoggerFactory().createLogger("KeelHttpServer");
    }

    public Logger getHttpServerLogger() {
        return httpServerLogger;
    }

    @Override
    public void close(Completable<Void> completion) {
        server.close()
              .andThen(ar -> {
                  if (ar.succeeded()) {
                      getHttpServerLogger().info(r -> r.message("HTTP Server Closed"));
                      afterShutdownServer()
                              .onComplete(completion);
                  } else {
                      getHttpServerLogger().exception(
                              ar.cause(),
                              r -> r.message("HTTP Server Closing Failure: %s"
                                      .formatted(ar.cause().getMessage()))
                      );
                      completion.fail(ar.cause());
                  }
              });
    }


    /**
     * Deploys the current verticle with an appropriate threading model configuration.
     * If virtual threads are available, the deployment options will be set to use the virtual threading model.
     *
     * @return a {@link Future} that completes with the deployment ID if the deployment is successful,
     *         or fails with an exception if the deployment fails.
     */
    public Future<String> deployMe() {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        if (ReflectionUtils.isVirtualThreadsAvailable()) {
            deploymentOptions.setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
        }
        return super.deployMe(deploymentOptions);
    }
}
