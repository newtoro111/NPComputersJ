package com.npcomputers.api;

import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ErrorHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> api(ApiException e) {
    return problem(e.status, e.code, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> invalid(MethodArgumentNotValidException e) {
    var r = problem(400, "VALIDATION", "Check the highlighted fields.");
    r.getBody()
        .setProperty(
            "fields",
            e.getBindingResult().getFieldErrors().stream()
                .map(f -> java.util.Map.of("field", f.getField(), "message", f.getDefaultMessage()))
                .toList());
    return r;
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ProblemDetail> malformed(Exception e) {
    return problem(400, "INVALID_REQUEST", "Invalid request format.");
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    ObjectOptimisticLockingFailureException.class
  })
  ResponseEntity<ProblemDetail> conflict(Exception e) {
    return problem(
        409, "CONFLICT", "The data changed or violates a uniqueness rule. Reload and try again.");
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ResponseEntity<ProblemDetail> forbidden(Exception e) {
    return problem(403, "FORBIDDEN", "This action is not permitted.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> unexpected(Exception e) {
    LoggerFactory.getLogger(getClass())
        .error("Unhandled request failure type={}", e.getClass().getSimpleName());
    return problem(
        503,
        "UNAVAILABLE",
        "The request could not be completed. Retry with the same order key if applicable.");
  }

  static ResponseEntity<ProblemDetail> problem(int status, String code, String message) {
    var p = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), message);
    p.setTitle(code);
    p.setType(java.net.URI.create("urn:npcomputers:error:" + code.toLowerCase()));
    p.setProperty("code", code);
    p.setProperty("correlationId", MDC.get("correlationId"));
    return ResponseEntity.status(status).body(p);
  }
}
