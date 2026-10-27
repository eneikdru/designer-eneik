package com.eneik.generated.designer.service;

/**
 * Exception thrown when the Playwright Vision Harness exceeds the defined rendering timeout
 * while processing a DOM document layout across viewports.
 */
public class RenderTimeoutException extends RuntimeException {

    public RenderTimeoutException(String message) {
        super(message);
    }

    public RenderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
