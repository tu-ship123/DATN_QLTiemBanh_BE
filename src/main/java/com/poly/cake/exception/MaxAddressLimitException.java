package com.poly.cake.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MaxAddressLimitException extends RuntimeException {
    public MaxAddressLimitException(String message) {
        super(message);
    }
}