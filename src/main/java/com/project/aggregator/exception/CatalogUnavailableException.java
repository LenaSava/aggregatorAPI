package com.project.aggregator.exception;

/**
 * Thrown when the Catalog service (a required dependency) is unavailable.
 */
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(Throwable cause) {
        super("Catalog service is unavailable", cause);
    }
}
