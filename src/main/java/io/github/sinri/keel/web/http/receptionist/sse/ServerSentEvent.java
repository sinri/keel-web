package io.github.sinri.keel.web.http.receptionist.sse;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A mutable SSE event with fluent setters. Set data or comment before encoding or sending.
 * An unset optional field is omitted; an empty id resets the cursor.
 * Instances must not be modified concurrently with encoding or sending.
 * @since 5.0.4
 */
@NullMarked
public final class ServerSentEvent {
    private @Nullable String comment;
    private @Nullable String data;
    private @Nullable String event;
    private @Nullable String id;
    private @Nullable Long retry;

    private static void validate(String value) {
        Objects.requireNonNull(value);
        if (value.contains("\r") || value.contains("\n")) {
            throw new IllegalArgumentException("Line break in SSE field");
        }
    }

    /** Sets a comment, which may be sent alone or alongside event data. */
    public ServerSentEvent comment(String comment) {
        this.comment = Objects.requireNonNull(comment);
        return this;
    }

    public ServerSentEvent data(String data) {
        this.data = Objects.requireNonNull(data);
        return this;
    }

    public ServerSentEvent event(String event) {
        validate(event);
        this.event = event;
        return this;
    }

    public ServerSentEvent id(String id) {
        validate(id);
        if (id.indexOf(0) >= 0) throw new IllegalArgumentException("NUL in id");
        this.id = id;
        return this;
    }

    public ServerSentEvent retry(long retry) {
        if (retry < 0) throw new IllegalArgumentException("Negative retry");
        this.retry = retry;
        return this;
    }

    public String encode() {
        if (comment == null) Objects.requireNonNull(data, "data or comment is required");
        StringBuilder out = new StringBuilder();
        if (comment != null) appendLines(out, ": ", comment);
        if (id != null) out.append("id: ").append(id).append('\n');
        if (event != null) out.append("event: ").append(event).append('\n');
        if (retry != null) out.append("retry: ").append(retry).append('\n');
        if (data != null) appendLines(out, "data: ", data);
        return out.append('\n').toString();
    }

    private static void appendLines(StringBuilder out, String prefix, String value) {
        for (String line : value.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            out.append(prefix).append(line).append('\n');
        }
    }
}
