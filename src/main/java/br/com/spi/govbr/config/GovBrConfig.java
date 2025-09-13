package br.com.spi.govbr.config;

public class GovBrConfig {

    // URLs
    public static final String NIVEL_API_URL = "https://sss/v1/govbr/nivel";
    public static final String LOGOUT_URL = "https://sss/openid-connect/logout";

    // Provider
    public static final String PROVIDER_ALIAS = "gov-br";
    public static final String AUTHENTICATOR_ID = "govbr-level-validator";
    public static final String AUTHENTICATOR_NAME = "Gov.br Level Validator";

    // Timeouts
    public static final int REQUEST_TIMEOUT = 30;
    public static final int CONNECT_TIMEOUT = 10;

    // Níveis aceitos (apenas Ouro)
    public static final String[] ACCEPTED_LEVELS = {"Ouro"};

    private GovBrConfig() {}
}