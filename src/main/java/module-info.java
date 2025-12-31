module io.github.sinri.keel.web {
    // Dependencies
    requires transitive io.github.sinri.keel.base;
    requires transitive io.github.sinri.keel.core;
    requires transitive io.github.sinri.keel.logger.api;
    requires transitive io.vertx.auth.common;
    requires transitive io.vertx.core;
    requires transitive io.vertx.web;
    requires transitive io.vertx.web.client;
    requires static org.jetbrains.annotations; // compile-time only

    // Public API exports
    exports io.github.sinri.keel.web.http;
    exports io.github.sinri.keel.web.http.fastdocs;
    // exports io.github.sinri.keel.web.http.fastdocs.page;
    exports io.github.sinri.keel.web.http.prehandler;
    exports io.github.sinri.keel.web.http.receptionist;
    exports io.github.sinri.keel.web.http.receptionist.responder;
    exports io.github.sinri.keel.web.http.requester.error;
    exports io.github.sinri.keel.web.http.requester.extractor;
    exports io.github.sinri.keel.web.tcp;
    exports io.github.sinri.keel.web.udp;
}