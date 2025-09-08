package br.com.spi.govbr.constants;

/**
 * Constantes para níveis de autenticação Gov.br
 */
public class GovBrLevelConstants {

    // Níveis de autenticação (nomes padronizados)
    public static final String BRONZE = "Bronze";
    public static final String PRATA = "Prata";
    public static final String OURO = "Ouro";

    // Códigos de nível retornados pela API Gov.br
    public static final String NIVEL_BRONZE_CODE = "1";
    public static final String NIVEL_PRATA_CODE = "2";
    public static final String NIVEL_OURO_CODE = "3";

    // Headers HTTP para requisições
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ACCEPT_HEADER = "Accept";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String USER_AGENT_VALUE = "Keycloak-GovBr-Validator/1.0";

    // Códigos de status HTTP
    public static final int HTTP_OK = 200;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_INTERNAL_ERROR = 500;

    // Chaves para logs estruturados
    public static final String LOG_USER_ID = "userId";
    public static final String LOG_PROVIDER = "provider";
    public static final String LOG_LEVEL = "level";
    public static final String LOG_STATUS = "status";

    private GovBrLevelConstants() {
        // Classe utilitária - construtor privado
    }
}