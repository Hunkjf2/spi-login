package br.com.spi.govbr.exception;

public class GovBrValidationException extends Exception {

    public GovBrValidationException(String message) {
        super(message);
    }

    public GovBrValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}