package com.poly.cake.exception;

public class MailSendingException extends RuntimeException {
    public MailSendingException(String message) {
        super(message);
    }
    public MailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}