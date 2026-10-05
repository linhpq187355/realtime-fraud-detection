package com.fraud.decisionservice.exception;

public class FeatureStateUnavailableException extends RuntimeException {
    public FeatureStateUnavailableException(String message) {
        super(message);
    }

    public FeatureStateUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
