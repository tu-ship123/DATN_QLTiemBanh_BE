package com.poly.cake.exception;

public class NgoaiLeGuiEmail extends RuntimeException {
    public NgoaiLeGuiEmail(String message) {
        super(message);
    }
    public NgoaiLeGuiEmail(String message, Throwable cause) {
        super(message, cause);
    }
}