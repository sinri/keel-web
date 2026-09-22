package io.github.sinri.keel.web.http.fastdocs.page;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Encoding of resource paths used by FastDocs links and requests.
 */
public final class FastDocsPathCodec {
    private FastDocsPathCodec() { }

    /**
     * Decodes percent escapes exactly once, preserving literal plus signs in URL paths.
     *
     * @param path encoded URL path
     * @return decoded resource path
     * @throws IllegalArgumentException if a percent escape is malformed
     */
    public static String decodePath(String path) {
        return URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    /**
     * Encodes resource names while retaining slashes between path segments.
     *
     * @param path decoded resource path, excluding the configured URL root
     * @return percent-encoded path
     */
    public static String encodePath(String path) {
        return Arrays.stream(path.split("/", -1))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8)
                        .replace("+", "%20").replace("*", "%2A").replace("%7E", "~"))
                .collect(Collectors.joining("/"));
    }
}
