package com.mtnrs.revenuesync.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_LATIN     = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE    = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_HYPHEN  = Pattern.compile("-{2,}");
    private static final Pattern EDGE_HYPHEN   = Pattern.compile("^-|-$");

    private SlugUtils() {}

    /**
     * Converts a plain string into a URL-safe slug.
     * Examples:
     *   "João da Silva"  → "joao-da-silva"
     *   "Backend Dev #1" → "backend-dev-1"
     */
    public static String slugify(String input) {
        if (input == null || input.isBlank()) return "";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return NON_LATIN.matcher(
                MULTI_HYPHEN.matcher(
                        EDGE_HYPHEN.matcher(
                                NON_LATIN.matcher(
                                        WHITESPACE.matcher(normalized.toLowerCase(Locale.ROOT))
                                                  .replaceAll("-")
                                ).replaceAll("")
                        ).replaceAll("")
                ).replaceAll("-")
        ).replaceAll("").trim();
    }

    /**
     * Appends a numeric suffix to guarantee uniqueness.
     * Example: "marcos-silva" + 2 → "marcos-silva-2"
     */
    public static String slugifyWithSuffix(String input, long suffix) {
        return slugify(input) + "-" + suffix;
    }
}
