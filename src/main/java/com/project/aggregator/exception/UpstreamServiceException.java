package com.project.aggregator.exception;

import lombok.Getter;

@Getter
public class UpstreamServiceException extends RuntimeException {

    private final String serviceName;

    public UpstreamServiceException(String serviceName, String message) {
        super("[" + serviceName + "] " + message);
        this.serviceName = serviceName;
    }

}
