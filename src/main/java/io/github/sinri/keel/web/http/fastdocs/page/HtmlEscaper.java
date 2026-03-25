package io.github.sinri.keel.web.http.fastdocs.page;

import io.github.sinri.keel.core.utils.StringUtils;

/**
 * Utility for escaping strings before insertion into HTML to prevent XSS.
 * <p>
 * Note: {@code StringUtils.escapeForHttpEntity()} from keel-core is not sufficient here
 * because it does not escape {@code "} and {@code '}, which are critical for preventing
 * attribute injection in HTML (e.g. {@code href='...'}).
 *
 * @since 5.0.1
 */
final class HtmlEscaper {
    private HtmlEscaper() {
    }

    /**
     * Escapes HTML special characters: {@code & < > " '}.
     * todo: it might be replaced by {@link StringUtils#escapeForHttpEntity(String)} when keel-core is upgraded to 5.0.2.
     */
    static String escape(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
