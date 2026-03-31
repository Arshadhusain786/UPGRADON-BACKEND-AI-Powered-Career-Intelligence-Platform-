package com.nexpath.exceptions;

public class InsufficientCreditsException extends RuntimeException {
    public InsufficientCreditsException() {
        super("Insufficient credits. Please purchase more to continue.");
    }
}
