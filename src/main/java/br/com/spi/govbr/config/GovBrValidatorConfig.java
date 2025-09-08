package br.com.spi.govbr.config;

/**
 * Configurações centralizadas para o validador de nível Gov.br
 */
public class GovBrValidatorConfig {

    // URLs da API Gov.br
    private static final String NIVEL_API_URL = "https://sso.teste/nivel";

    // Configurações do Provider
    private static final String GOVBR_PROVIDER_ALIAS = "gov-br";
    private static final String AUTHENTICATOR_PROVIDER_ID = "govbr-level-validator";
    private static final String AUTHENTICATOR_DISPLAY_TYPE = "Gov.br Level Validator";

    // Timeouts HTTP (em segundos)
    private static final int REQUEST_TIMEOUT = 30;
    private static final int CONNECT_TIMEOUT = 10;

    // Níveis mínimos aceitos para login
    private static final String[] ACCEPTED_LEVELS = {"Prata", "Ouro"};

    // Mensagens de erro
    private static final String ERROR_INSUFFICIENT_LEVEL =
            "Nível de autenticação insuficiente. É necessário nível Prata ou Ouro para acessar este sistema.";
    private static final String ERROR_API_UNAVAILABLE =
            "Serviço de validação Gov.br temporariamente indisponível. Tente novamente em alguns minutos.";
    private static final String ERROR_INVALID_TOKEN =
            "Token de autenticação Gov.br inválido ou expirado.";

    private GovBrValidatorConfig() {
        // Classe utilitária - construtor privado
    }

    // Getters para URLs
    public static String getNivelApiUrl() {
        return NIVEL_API_URL;
    }

    // Getters para configurações do provider
    public static String getGovBrProviderAlias() {
        return GOVBR_PROVIDER_ALIAS;
    }

    public static String getAuthenticatorProviderId() {
        return AUTHENTICATOR_PROVIDER_ID;
    }

    public static String getAuthenticatorDisplayType() {
        return AUTHENTICATOR_DISPLAY_TYPE;
    }

    // Getters para timeouts
    public static int getRequestTimeout() {
        return REQUEST_TIMEOUT;
    }

    public static int getConnectTimeout() {
        return CONNECT_TIMEOUT;
    }

    // Getters para validação
    public static String[] getAcceptedLevels() {
        return ACCEPTED_LEVELS.clone();
    }

    // Getters para mensagens de erro
    public static String getErrorInsufficientLevel() {
        return ERROR_INSUFFICIENT_LEVEL;
    }

    public static String getErrorApiUnavailable() {
        return ERROR_API_UNAVAILABLE;
    }

    public static String getErrorInvalidToken() {
        return ERROR_INVALID_TOKEN;
    }
}