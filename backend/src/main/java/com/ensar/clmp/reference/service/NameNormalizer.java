package com.ensar.clmp.reference.service;

import java.util.Locale;

/** lower(trim(collapse internal whitespace)) for vendor, client, and job-title matching (FR-051, FR-055). */
public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Display form: trimmed, otherwise as first entered. */
    public static String display(String value) {
        return value == null ? null : value.trim();
    }
}
