package com.qflow.hello.api.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloErrorController implements ErrorController {
    @RequestMapping("${spring.web.error.path:${error.path:/error}}")
    public ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
        Object errorStatus = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatusCode status =
                errorStatus instanceof Integer code
                        ? HttpStatusCode.valueOf(code)
                        : HttpStatus.NOT_FOUND;
        Object errorPath = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path =
                errorPath instanceof String originalPath ? originalPath : request.getRequestURI();
        return new ResponseEntity<>(
                HelloProblemDetails.create(status, path, request),
                HelloProblemDetails.headers(HttpHeaders.EMPTY, request),
                status);
    }
}
