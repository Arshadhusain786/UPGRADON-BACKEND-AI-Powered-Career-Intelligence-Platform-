package com.nexpath.exceptions;

public class DuplicateRequestException extends RuntimeException {
    public DuplicateRequestException() {
        super("You have already sent a connection request to this opportunity.");
    }
}
