package com.ensar.clmp.common.error;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A business-rule refusal. The {@link ErrorCode} decides the HTTP status; {@code extras} become
 * extension properties on the ProblemDetail (for example {@code missingItems}).
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> extras;
    private final List<FieldErrorItem> fieldErrors;

    public BusinessException(ErrorCode code, String detail) {
        this(code, detail, Map.of(), List.of());
    }

    public BusinessException(ErrorCode code, String detail, Map<String, Object> extras) {
        this(code, detail, extras, List.of());
    }

    public BusinessException(ErrorCode code, String detail, Map<String, Object> extras,
            List<FieldErrorItem> fieldErrors) {
        super(detail);
        this.code = code;
        this.extras = new LinkedHashMap<>(extras);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    /** A 400 VALIDATION_FAILED for a single field, for rules bean validation cannot express. */
    public static BusinessException fieldError(String field, String message) {
        return new BusinessException(ErrorCode.VALIDATION_FAILED, "One or more fields are invalid.",
                Map.of(), List.of(new FieldErrorItem(field, message)));
    }

    public ErrorCode getCode() {
        return code;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
