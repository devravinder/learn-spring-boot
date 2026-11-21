package com.paravar.rest.errors;

import com.paravar.rest.users.DuplicateEmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// extending ResponseEntityExceptionHandler keeps RFC 9457 bodies for every Spring MVC exception
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // body validation: one entry per field so a form can show messages next to inputs
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail p = ex.getBody();
        p.setType(URI.create("https://example.test/problems/validation"));
        p.setTitle("Validation failed");
        p.setDetail(ex.getErrorCount() + " field(s) are invalid");
        List<Map<String, Object>> errors = ex.getBindingResult().getAllErrors().stream()
                .map(e -> Map.<String, Object>of("field", e instanceof FieldError f ? f.getField() : e.getObjectName(),
                        "message", String.valueOf(e.getDefaultMessage())))
                .toList();
        p.setProperty("errors", errors);
        return handleExceptionInternal(ex, p, headers, status, request);
    }

    // parameter validation (@Min on @PathVariable / @RequestParam)
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers,
                                                                            HttpStatusCode status, WebRequest request) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameters");
        p.setType(URI.create("https://example.test/problems/validation"));
        p.setTitle("Validation failed");
        List<Map<String, Object>> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream().map(e -> Map.<String, Object>of(
                        "field", String.valueOf(r.getMethodParameter().getParameterName()),
                        "message", String.valueOf(((MessageSourceResolvable) e).getDefaultMessage()))))
                .toList();
        p.setProperty("errors", errors);
        return handleExceptionInternal(ex, p, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ProblemDetail duplicate(DuplicateEmailException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setType(URI.create("https://example.test/problems/duplicate-email"));
        p.setTitle("Duplicate email");
        p.setProperty("email", e.email());
        return p;
    }

    // last resort: log the details with an id, return only the id
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        String errorId = UUID.randomUUID().toString();
        log.error("unexpected error {}", errorId, e);
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Quote the error id.");
        p.setTitle("Internal error");
        p.setProperty("errorId", errorId);
        return p;
    }
}
