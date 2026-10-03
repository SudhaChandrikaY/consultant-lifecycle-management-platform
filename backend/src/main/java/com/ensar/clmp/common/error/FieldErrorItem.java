package com.ensar.clmp.common.error;

/** One field-level validation message, rendered next to the matching form field (FR-104). */
public record FieldErrorItem(String field, String message) {
}
