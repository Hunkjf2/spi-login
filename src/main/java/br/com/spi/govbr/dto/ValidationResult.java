package br.com.spi.govbr.dto;

/**
 * Resultado da validação de nível Gov.br
 */
public record ValidationResult(
        boolean isValid,
        String userLevel,
        String errorMessage,
        String errorCode
) {

    /**
     * Cria resultado de sucesso
     */
    public static ValidationResult success(String userLevel) {
        return new ValidationResult(true, userLevel, null, null);
    }

    /**
     * Cria resultado de falha com nível conhecido
     */
    public static ValidationResult failure(String userLevel, String errorMessage, String errorCode) {
        return new ValidationResult(false, userLevel, errorMessage, errorCode);
    }

    /**
     * Cria resultado de erro (sem nível conhecido)
     */
    public static ValidationResult error(String errorMessage, String errorCode) {
        return new ValidationResult(false, null, errorMessage, errorCode);
    }

    /**
     * Verifica se houve erro de API
     */
    public boolean hasApiError() {
        return !isValid && userLevel == null;
    }
}