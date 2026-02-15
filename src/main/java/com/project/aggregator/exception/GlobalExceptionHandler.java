package com.project.aggregator.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Centralises HTTP error mapping so the controller stays clean.
 * We use RFC 9457 ProblemDetail (built into Spring 6 / Boot 3) — it's the
 * modern standard and gives clients a consistent error envelope.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Product does not exist → 404.
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getProductId());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        pd.setType(URI.create("urn:kramp:error:product-not-found"));
        pd.setTitle("Product Not Found");
        return pd;
    }

    /**
     * Catalog service (required) is down → 503.
     * We bubble this up as a hard failure because we cannot render a product
     * page without basic product information.
     */
    @ExceptionHandler(CatalogUnavailableException.class)
    public ProblemDetail handleCatalogUnavailable(CatalogUnavailableException ex) {
        log.error("Catalog service unavailable: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to retrieve product information. Please try again shortly."
        );
        pd.setType(URI.create("urn:kramp:error:catalog-unavailable"));
        pd.setTitle("Catalog Unavailable");
        return pd;
    }

    /**
     * Bean-validation on @RequestBody / @ModelAttribute → 400.
     * BindException is the parent of MethodArgumentNotValidException,
     * so this catches both @ModelAttribute and @RequestBody validation failures.
     */
    @ExceptionHandler(BindException.class)
    public ProblemDetail handleBindException(BindException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", detail);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(URI.create("urn:kramp:error:validation"));
        pd.setTitle("Validation Error");
        return pd;
    }

    /**
     * Bean-validation on @PathVariable / @RequestParam (@Validated controller) → 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", detail);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(URI.create("urn:kramp:error:validation"));
        pd.setTitle("Validation Error");
        return pd;
    }

    /**
     * Safety net for anything else.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred."
        );
        pd.setType(URI.create("urn:kramp:error:internal"));
        pd.setTitle("Internal Server Error");
        return pd;
    }
}
