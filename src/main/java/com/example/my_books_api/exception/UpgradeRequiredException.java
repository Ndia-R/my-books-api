package com.example.my_books_api.exception;

public class UpgradeRequiredException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UpgradeRequiredException() {
    }

    public UpgradeRequiredException(String message) {
        super(message);
    }

    public UpgradeRequiredException(Throwable cause) {
        super(cause);
    }

    public UpgradeRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
