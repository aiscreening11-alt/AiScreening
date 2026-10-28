package com.company.recruitment.util;

import java.util.regex.Pattern;

public final class PiiSanitizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[- .]?)?\\(?\\d{3}\\)?[- .]?\\d{3}[- .]?\\d{4}|(?:\\+?\\d{1,3}[- .]?)?\\d{10}"
    );

    private PiiSanitizer() {
    }

    public static String sanitize(String text) {
        if (text == null) return "";
        String sanitized = EMAIL_PATTERN.matcher(text).replaceAll("[REDACTED_EMAIL]");
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("[REDACTED_PHONE]");
        return sanitized;
    }
}
