package io.github.sinri.keel.web.tcp.piece;

import io.vertx.core.buffer.Buffer;

/**
 *
 * @since 5.0.0
 */
public interface BufferPiece {
    Buffer toBuffer();

    default int getLength() {
        return toBuffer().length();
    }
}
