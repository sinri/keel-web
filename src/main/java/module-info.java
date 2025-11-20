module io.github.sinri.keel.web {
    // Dependencies
    requires io.github.sinri.keel.base;
    requires io.github.sinri.keel.core;
    requires io.github.sinri.keel.logger.api;
    requires io.vertx.auth.common;
    requires io.vertx.core;
    requires io.vertx.core.logging;
    requires io.vertx.web;
    requires io.vertx.web.client;
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