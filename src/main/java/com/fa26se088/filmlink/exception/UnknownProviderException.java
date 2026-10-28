package com.fa26se088.filmlink.exception;

public class UnknownProviderException extends RuntimeException {
    public UnknownProviderException(String code) {
        super("Không có delivery provider: " + code);
    }
}
