package br.com.spi.govbr.config;

/**
 * Configurações centralizadas para o validador de nível Gov.br
 */
public class GovBrValidatorConfig {

    private static final String NIVEL_API_URL = "https://sso.teste/nivel";
    private static final String GOVBR_PROVIDER_ALIAS = "gov-br";
    private static final String AUTHENTICATOR_PROVIDER_ID = "govbr-level-validator";
    private static final String AUTHENTICATOR_DISPLAY_TYPE = "Gov.br Level Validator";
    private static final int REQUEST_TIMEOUT = 30; // segundos
    private static final int CONNECT_TIMEOUT = 10; // segundos

    // Níveis mínimos aceitos
    private static final String[] ACCEPTED_LEVELS = {"Prata", "Ouro"};

    private GovBrValidatorConfig() {}

    public static String getNivelApiUrl() { return NIVEL_API_URL; }
    public static String getGovBrProviderAlias() { return GOVBR_PROVIDER_ALIAS; }
    public static String getAuthenticatorProviderId() { return AUTHENTICATOR_PROVIDER_ID; }
    public static String getAuthenticatorDisplayType() { return AUTHENTICATOR_DISPLAY_TYPE; }
    public static int getRequestTimeout() { return REQUEST_TIMEOUT; }
    public static int getConnectTimeout() { return CONNECT_TIMEOUT; }
    public static String[] getAcceptedLevels() { return ACCEPTED_LEVELS.clone(); }
}