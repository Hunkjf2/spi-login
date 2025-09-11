package br.com.spi.govbr.config;

/**
 * Configurações centralizadas para o validador de nível Gov.br
 */
public class GovBrValidatorConfig {

    // URLs da API Gov.br
    private static final String NIVEL_API_URL = "https://nivel-loginpa.openshift.homologar.prodepa.pa.gov.br/v1/govbr/nivel";
    private static final String GOVBR_PROVIDER_ALIAS = "gov-br";
    private static final String AUTHENTICATOR_PROVIDER_ID = "govbr-level-validator";
    private static final String AUTHENTICATOR_DISPLAY_TYPE = "Gov.br Level Validator";
    private static final int REQUEST_TIMEOUT = 30;
    private static final int CONNECT_TIMEOUT = 10;
    // Modificação: Apenas Prata e Ouro são aceitos (Bronze removido)
    private static final String[] ACCEPTED_LEVELS = {"Bronze", "Ouro"};

    // Mensagens de erro mais específicas para redirecionamento ao login
    private static final String ERROR_INSUFFICIENT_LEVEL =
            "Nível de autenticação insuficiente. É necessário ter nível Prata ou Ouro no Gov.br para acessar este sistema. Acesse gov.br para elevar seu nível de confiabilidade.";

    private static final String ERROR_API_UNAVAILABLE =
            "Não foi possível validar seu nível Gov.br no momento. Tente fazer login novamente em alguns minutos.";

    private static final String ERROR_INVALID_TOKEN =
            "Sua sessão Gov.br expirou ou é inválida. Faça login novamente através do Gov.br.";

    private static final String ERROR_BRONZE_LEVEL =
            "Seu nível Bronze no Gov.br não é suficiente para acessar este sistema. É necessário nível Prata ou Ouro. Acesse gov.br para elevar seu nível de confiabilidade.";

    private GovBrValidatorConfig() {}

    public static String getNivelApiUrl() {
        return NIVEL_API_URL;
    }

    public static String getGovBrProviderAlias() {
        return GOVBR_PROVIDER_ALIAS;
    }

    public static String getAuthenticatorProviderId() {
        return AUTHENTICATOR_PROVIDER_ID;
    }

    public static String getAuthenticatorDisplayType() {
        return AUTHENTICATOR_DISPLAY_TYPE;
    }

    public static int getRequestTimeout() {
        return REQUEST_TIMEOUT;
    }

    public static int getConnectTimeout() {
        return CONNECT_TIMEOUT;
    }

    public static String[] getAcceptedLevels() {
        return ACCEPTED_LEVELS.clone();
    }

    public static String getErrorInsufficientLevel() {
        return ERROR_INSUFFICIENT_LEVEL;
    }

    public static String getErrorApiUnavailable() {
        return ERROR_API_UNAVAILABLE;
    }

    public static String getErrorInvalidToken() {
        return ERROR_INVALID_TOKEN;
    }

    public static String getErrorBronzeLevel() {
        return ERROR_BRONZE_LEVEL;
    }
}