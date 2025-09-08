package br.com.spi.govbr.exception;

/**
 * Exception específica para erros de validação Gov.br
 */
public class GovBrValidationException extends Exception {

    private final String errorCode;

    public GovBrValidationException(String message) {
        super(message);
        this.errorCode = "UNKNOWN_ERROR";
    }

    public GovBrValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public GovBrValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN_ERROR";
    }

    public GovBrValidationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Factory methods para tipos específicos de erro
     */
    public static GovBrValidationException apiUnavailable(Throwable cause) {
        return new GovBrValidationException(
                "API Gov.br temporariamente indisponível",
                "API_UNAVAILABLE",
                cause
        );
    }

    public static GovBrValidationException invalidToken() {
        return new GovBrValidationException(
                "Token Gov.br inválido ou expirado",
                "INVALID_TOKEN"
        );
    }

    public static GovBrValidationException insufficientLevel(String currentLevel) {
        return new GovBrValidationException(
                String.format("Nível %s insuficiente para acesso", currentLevel),
                "INSUFFICIENT_LEVEL"
        );
    }
}