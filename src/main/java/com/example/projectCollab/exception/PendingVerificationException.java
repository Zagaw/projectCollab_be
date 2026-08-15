package com.example.projectCollab.exception;

public class PendingVerificationException extends RuntimeException {
    public PendingVerificationException(String message) {
        super(message);
    }
}