package com.techhub.common;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtils {

    private static final Pattern DIACRITICAL_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MULTI_HYPHENS = Pattern.compile("-+");
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SlugUtils() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "product";
        }

        String normalized = input.replace("đ", "d").replace("Đ", "d");
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = DIACRITICAL_MARKS.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        normalized = MULTI_HYPHENS.matcher(normalized).replaceAll("-");

        // Trim leading and trailing hyphens
        if (normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("-")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.length() > 200) {
            normalized = normalized.substring(0, 200);
            if (normalized.endsWith("-")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
        }

        return normalized.isEmpty() ? "product" : normalized;
    }

    public static String generateShortId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public static String generateSlug(String name) {
        return slugify(name) + "-" + generateShortId(6);
    }
}
