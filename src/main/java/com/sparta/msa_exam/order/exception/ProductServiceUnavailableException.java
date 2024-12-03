package com.sparta.msa_exam.order.exception;

public class ProductServiceUnavailableException extends RuntimeException {
    public ProductServiceUnavailableException(String message) {
        super(message);
    }
}