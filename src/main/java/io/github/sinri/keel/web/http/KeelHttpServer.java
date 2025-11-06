package io.github.sinri.keel.web.http;

import io.github.sinri.keel.core.verticles.KeelVerticleImpl;
import io.github.sinri.keel.logger.event.KeelEventLog;
import io.github.sinri.keel.logger.issue.center.KeelIssueRecordCenter;
import io.github.sinri.keel.logger.issue.recorder.KeelIssueRecorder;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import javax.annotation.Nonnull;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

abstract public class KeelHttpServer extends KeelVerticleImpl implements Closeable {
    public static final String CONFIG_HTTP_SERVER_PORT = "http_server_port";
    public static final String CONFIG_HTTP_SERVER_OPTIONS = "http_server_options";
    protected HttpServer server;
    private KeelIssueRecorder<KeelEventLog> httpServerLogger;

    protected int getHttpServerPort() {
        return this.config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);
    }

    protected HttpServerOptions getHttpServerOptions() {
        JsonObject httpServerOptions = this.config().getJsonObject(CONFIG_HTTP_SERVER_OPTIONS);
        if (httpServerOptions == null) {
            return new HttpServerOptions()
                    .setPort(getHttpServerPort());
        } else {
            return new HttpServerOptions(httpServerOptions);
        }
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
     * @since 4.1.3
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
     * @since 4.1.3
     */
    protected Future<Void> afterShutdownServer() {
        return Future.succeededFuture();
    }

    @Override
    protected Future<Void> startVerticle() {
        this.httpServerLogger = buildHttpServerIssueRecorder();

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
     * @since 4.1.5
     */
    public KeelIssueRecordCenter getIssueRecordCenter() {
        return KeelIssueRecordCenter.outputCenter();
    }

    /**
     * @since 4.0.2
     */
    @Nonnull
    protected KeelIssueRecorder<KeelEventLog> buildHttpServerIssueRecorder() {
        return getIssueRecordCenter().generateIssueRecorder("KeelHttpServer", KeelEventLog::new);
    }

    /**
     * @since 4.0.2
     */
    public KeelIssueRecorder<KeelEventLog> getHttpServerLogger() {
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
                      getHttpServerLogger().exception(ar.cause(),
                              r -> r.message("HTTP Server Closing Failure: " + ar.cause()
                                                                                 .getMessage()));
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
     * @since 4.1.3
     */
    public Future<String> deployMe() {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        if (Keel.reflectionHelper().isVirtualThreadsAvailable()) {
            deploymentOptions.setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
        }
        return super.deployMe(deploymentOptions);
    }
}
