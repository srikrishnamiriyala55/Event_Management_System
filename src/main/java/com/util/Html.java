package com.util;

/** Escapes text for HTML content and quoted HTML attributes. */
public final class Html {

    private Html() {
    }

    public static String escape(Object value) {
        return value == null ? "" : value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
