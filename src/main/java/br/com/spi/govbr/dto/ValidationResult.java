package br.com.spi.govbr.dto;

public record ValidationResult(
        boolean isValid,
        String userLevel,
        String errorMessage
) {
    public static ValidationResult success(String userLevel) {
        return new ValidationResult(true, userLevel, null);
    }

    public static ValidationResult failure(String userLevel, String errorMessage) {
        return new ValidationResult(false, userLevel, errorMessage);
    }

    public static ValidationResult error(String errorMessage) {
        return new ValidationResult(false, null, errorMessage);
    }
}