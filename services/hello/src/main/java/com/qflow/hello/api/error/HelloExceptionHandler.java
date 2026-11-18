package com.qflow.hello.api.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class HelloExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(Exception.class)
    public @Nullable ResponseEntity<Object> handleUnexpectedException(
            Exception ex, WebRequest request) {
        if (ex instanceof ErrorResponse error) {
            return handleExceptionInternal(
                    ex, null, error.getHeaders(), error.getStatusCode(), request);
        }
        ResponseStatus annotation =
                AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        HttpStatus status =
                annotation != null ? annotation.code() : HttpStatus.INTERNAL_SERVER_ERROR;
        return handleExceptionInternal(ex, null, HttpHeaders.EMPTY, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        ProblemDetail problem =
                HelloProblemDetails.create(status, servletRequest.getRequestURI(), servletRequest);
        if (status.value() == 400) {
            addValidationErrors(ex, problem);
        }
        if (status.is5xxServerError()) {
            logger.error(
                    "Hello request failed, correlationId="
                            + problem.getProperties().get("correlationId"),
                    ex);
        }
        return super.handleExceptionInternal(
                ex, problem, HelloProblemDetails.headers(headers, servletRequest), status, request);
    }

    private void addValidationErrors(Exception ex, ProblemDetail problem) {
        List<ValidationError> errors = new ArrayList<>();
        if (ex instanceof MethodArgumentNotValidException validation) {
            validation.getFieldErrors().forEach(error -> errors.add(toValidationError(error)));
        } else if (ex instanceof HandlerMethodValidationException validation) {
            for (var result : validation.getParameterValidationResults()) {
                if (result instanceof ParameterErrors fields) {
                    fields.getFieldErrors().forEach(error -> errors.add(toValidationError(error)));
                } else {
                    var parameter = result.getMethodParameter();
                    String field = parameter.getParameterName();
                    String name = field != null ? field : "arg" + parameter.getParameterIndex();
                    for (var error : result.getResolvableErrors()) {
                        String[] codes = error.getCodes();
                        String code =
                                codes != null && codes.length > 0
                                        ? codes[codes.length - 1]
                                        : "Invalid";
                        errors.add(new ValidationError(name, code, "Invalid value."));
                    }
                }
            }
        } else {
            return;
        }
        problem.setType(URI.create("https://qflow.local/problems/validation-error"));
        problem.setTitle("Validation failed");
        problem.setDetail("Request contains invalid fields");
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
    }

    private ValidationError toValidationError(FieldError error) {
        String code = error.getCode() != null ? error.getCode() : "Invalid";
        return new ValidationError(error.getField(), code, "Invalid value.");
    }

    private record ValidationError(String field, String code, String message) {}
}
