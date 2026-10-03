package com.ensar.clmp.common.error;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every failure into an RFC 9457 ProblemDetail with a stable {@code code}. Request bodies
 * and field values are never echoed or logged (FR-007, constitution VII).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INVALID_FIELDS = "One or more fields are invalid.";

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        ProblemDetail problem = Problems.of(ex.getCode(), ex.getMessage());
        ex.getExtras().forEach(problem::setProperty);
        if (!ex.getFieldErrors().isEmpty()) {
            problem.setProperty("fieldErrors", ex.getFieldErrors());
        }
        return ResponseEntity.status(ex.getCode().status()).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorItem(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return validation(errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return validation(List.of(new FieldErrorItem(ex.getName(), "Invalid value.")));
    }

    /** A JPA {@code @Version} conflict that slipped past the explicit VersionGuard check. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problem = Problems.of(ErrorCode.CONCURRENT_MODIFICATION,
                "This record was changed by someone else. Reload to continue.");
        return ResponseEntity.status(ErrorCode.CONCURRENT_MODIFICATION.status()).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = Problems.of(ErrorCode.NOT_AUTHORIZED,
                "You are not authorized to perform this action.");
        return ResponseEntity.status(ErrorCode.NOT_AUTHORIZED.status()).body(problem);
    }

    @ExceptionHandler({ EntityNotFoundException.class, NoSuchElementException.class })
    ResponseEntity<ProblemDetail> handleNotFound(RuntimeException ex) {
        ProblemDetail problem = Problems.of(ErrorCode.NOT_FOUND, "The requested record does not exist.");
        return ResponseEntity.status(ErrorCode.NOT_FOUND.status()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        // Log the exception type and stack only; never request bodies or parameter values.
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal error");
        problem.setProperty("code", "INTERNAL_ERROR");
        return ResponseEntity.internalServerError().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return asObject(validation(errors));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        List<FieldErrorItem> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new FieldErrorItem(r.getMethodParameter().getParameterName(),
                                e.getDefaultMessage())))
                .toList();
        return asObject(validation(errors));
    }

    /** Adds a {@code code} to the framework-built problems (400 malformed body, 405, 404, ...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey("code"))) {
            problem.setProperty("code", codeFor(statusCode));
            if (statusCode.value() == 400) {
                problem.setDetail("The request could not be read.");
            }
        }
        return response;
    }

    private static String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> ErrorCode.VALIDATION_FAILED.name();
            case 401 -> ErrorCode.UNAUTHENTICATED.name();
            case 403 -> ErrorCode.NOT_AUTHORIZED.name();
            case 404 -> ErrorCode.NOT_FOUND.name();
            case 405 -> "METHOD_NOT_ALLOWED";
            default -> "HTTP_" + status.value();
        };
    }

    private static ResponseEntity<ProblemDetail> validation(List<FieldErrorItem> errors) {
        ProblemDetail problem = Problems.of(ErrorCode.VALIDATION_FAILED, INVALID_FIELDS);
        problem.setProperty("fieldErrors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static ResponseEntity<Object> asObject(ResponseEntity<ProblemDetail> response) {
        return (ResponseEntity) response;
    }

    private static String lastNode(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }
}
