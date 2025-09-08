package br.com.spi.govbr.constants;

public class GovBrLevelConstants {

    // Níveis de autenticação
    public static final String BRONZE = "Bronze";
    public static final String PRATA = "Prata";
    public static final String OURO = "Ouro";

    // Códigos de nível da API
    public static final String NIVEL_BRONZE_CODE = "1";
    public static final String NIVEL_PRATA_CODE = "2";
    public static final String NIVEL_OURO_CODE = "3";

    // Mensagens de erro
    public static final String ERROR_INSUFFICIENT_LEVEL = "govbr.error.insufficient.level";
    public static final String ERROR_API_UNAVAILABLE = "govbr.error.api.unavailable";
    public static final String ERROR_TOKEN_NOT_FOUND = "govbr.error.token.not.found";

    // Headers HTTP
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ACCEPT_HEADER = "Accept";
    public static final String CONTENT_TYPE_JSON = "application/json";

    private GovBrLevelConstants() {}
}