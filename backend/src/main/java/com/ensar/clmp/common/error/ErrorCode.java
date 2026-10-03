package com.ensar.clmp.common.error;

import org.springframework.http.HttpStatus;

/**
 * Stable machine-readable error codes from contracts/rest-api.md § Errors. Each code carries the
 * HTTP status it is returned with and a short human title.
 */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "Not authorized"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Record changed"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "Duplicate email"),
    INVALID_TRANSITION(HttpStatus.UNPROCESSABLE_CONTENT, "Invalid transition"),
    READINESS_INCOMPLETE(HttpStatus.UNPROCESSABLE_CONTENT, "Readiness incomplete"),
    CONFIRMATION_REQUIRED(HttpStatus.CONFLICT, "Confirmation required"),
    RECRUITER_INACTIVE(HttpStatus.UNPROCESSABLE_CONTENT, "Recruiter inactive"),
    CONSULTANT_NOT_ELIGIBLE(HttpStatus.UNPROCESSABLE_CONTENT, "Consultant not eligible"),
    OPEN_ASSIGNMENT_EXISTS(HttpStatus.CONFLICT, "Open assignment exists"),
    BUSINESS_RULE(HttpStatus.UNPROCESSABLE_CONTENT, "Business rule"),
    DUPLICATE_SUBMISSION(HttpStatus.CONFLICT, "Possible duplicate submission"),
    OPEN_SUBMISSIONS_EXIST(HttpStatus.UNPROCESSABLE_CONTENT, "Open submissions exist");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }
}
