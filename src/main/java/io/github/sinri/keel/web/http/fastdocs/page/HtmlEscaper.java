package io.github.sinri.keel.web.http.fastdocs.page;

/**
 * Utility for escaping strings before insertion into HTML to prevent XSS.
 *
 * @since 5.0.1
 */
final class HtmlEscaper {
    private HtmlEscaper() {
    }

    /**
     * Escapes HTML special characters: {@code & < > " '}.
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
