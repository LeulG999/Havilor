package com.leul.havilor.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;

// Turns every exception that leaves a controller into the same ApiError body.
// Controllers and services never build error responses themselves: they throw.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    // 400: @Valid found broken fields in the request body.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldViolation(f.getField(), f.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiError.FieldViolation::field)
                        .thenComparing(ApiError.FieldViolation::message))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, violations);
    }

    // 400: a constraint on a controller parameter failed, e.g. @Max(100) on ?count=500.
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> invalidParameter(HandlerMethodValidationException e, HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiError.FieldViolation(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, violations);
    }

    // 400: the body is missing, is not JSON, or a value can't be read (bad date, unknown enum value...).
    // Jackson's own message is not returned: it can mention internal class names.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException e, HttpServletRequest request) {
        if (e.getCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
            String field = fieldPath(jackson.getPath());
            return build(HttpStatus.BAD_REQUEST, "Request body has an unreadable value", request,
                    List.of(new ApiError.FieldViolation(field, "has an invalid value or format")));
        }
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", request, List.of());
    }

    // 400: a path or query parameter has the wrong type, e.g. /alerts/abc or ?page=x.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String expected = e.getRequiredType() == null ? "another type" : e.getRequiredType().getSimpleName();
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'", request,
                List.of(new ApiError.FieldViolation(e.getName(), "must be a " + expected)));
    }

    // 400: fields that are each valid but don't fit together (checked in code, not by annotations).
    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> badRequest(BadRequestException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request,
                List.of(new ApiError.FieldViolation(e.getField(), e.getMessage())));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, e.getMessage(), request, List.of());
    }

    // 409: @Version found that another request changed the row between our read and our write.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrentChange(OptimisticLockingFailureException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Changed by another request at the same time. Reload and try again.",
                request, List.of());
    }

    // 409: a database constraint refused the write. The only one a client can hit today is the unique
    // ticket number, when two requests create the same new ticket at the same moment.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> constraintViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Alert with this ticket number already exists", request, List.of());
    }

    // Everything else. Spring's own web exceptions (unknown URL 404, wrong method 405, wrong content
    // type 415, missing parameter 400...) carry a safe status and message, so we keep them.
    // Anything unexpected is a 500: the details go to the log, never to the client.
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> other(Exception e, HttpServletRequest request) {
        if (e instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            String message = springError.getBody().getDetail();
            return build(status, message != null ? message : reason(status), request, List.of());
        }
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatusCode status, String message,
                                                  HttpServletRequest request,
                                                  List<ApiError.FieldViolation> fieldErrors) {
        ApiError body = new ApiError(status.value(), reason(status), message,
                request.getRequestURI(), clock.instant(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private static String reason(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.getReasonPhrase() : "Error";
    }

    // ["recipients", 0, "address"] → "recipients[0].address"
    private static String fieldPath(List<JacksonException.Reference> path) {
        StringBuilder field = new StringBuilder();
        for (JacksonException.Reference ref : path) {
            if (ref.getPropertyName() != null) {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                field.append('[').append(ref.getIndex()).append(']');
            }
        }
        return field.toString();
    }
}
