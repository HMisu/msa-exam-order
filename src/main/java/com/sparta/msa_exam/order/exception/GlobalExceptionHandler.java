package com.sparta.msa_exam.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductServiceUnavailableException.class)
    public ResponseEntity<Object> handleProductServiceUnavailableException(ProductServiceUnavailableException ex) {
        RestApiException restApiException = new RestApiException(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE.value());
        return new ResponseEntity<>(restApiException, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
