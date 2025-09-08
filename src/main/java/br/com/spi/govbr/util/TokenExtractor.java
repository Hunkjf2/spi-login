package br.com.spi.govbr.util;

import br.com.spi.govbr.config.GovBrValidatorConfig;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.UserModel;

/**
 * Utilitário para extração de tokens Gov.br do contexto do Keycloak
 */
public class TokenExtractor {

    private static final Logger logger = Logger.getLogger(TokenExtractor.class);

    private TokenExtractor() {
        // Classe utilitária - construtor privado
    }

    /**
     * Extrai o token de acesso Gov.br do contexto de autenticação
     *
     * @param context Contexto de autenticação do Keycloak
     * @return Token de acesso ou null se não encontrado
     */
    public static String extrairTokenGovBr(AuthenticationFlowContext context) {
        try {
            logger.debug("Iniciando extração de token Gov.br");

            UserModel user = context.getUser();
            if (user == null) {
                logger.warn("Usuário não encontrado no contexto de autenticação");
                return null;
            }

            logger.debugf("Extraindo token para usuário: %s", user.getUsername());

            // Busca identidade federada Gov.br
            String tokenJson = context.getSession().users()
                    .getFederatedIdentitiesStream(context.getRealm(), user)
                    .filter(fed -> GovBrValidatorConfig.getGovBrProviderAlias()
                            .equals(fed.getIdentityProvider()))
                    .findFirst()
                    .map(FederatedIdentityModel::getToken)
                    .orElse(null);

            if (tokenJson == null || tokenJson.trim().isEmpty()) {
                logger.warn("Token federado Gov.br não encontrado para o usuário");
                return null;
            }

            String accessToken = extrairAccessToken(tokenJson);

            if (accessToken != null) {
                logger.debug("Token Gov.br extraído com sucesso");
                // Log apenas início e fim para segurança
                logger.debugf("Token prefix: %s...%s",
                        accessToken.substring(0, Math.min(10, accessToken.length())),
                        accessToken.length() > 10 ? accessToken.substring(accessToken.length() - 4) : ""
                );
            } else {
                logger.warn("Falha ao extrair access_token do JSON federado");
            }

            return accessToken;

        } catch (Exception e) {
            logger.errorf("Erro inesperado ao extrair token Gov.br: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Extrai o access_token do JSON de token federado
     *
     * @param tokenJson JSON contendo dados do token federado
     * @return Access token ou null se não encontrado
     */
    private static String extrairAccessToken(String tokenJson) {
        try {
            if (tokenJson == null || tokenJson.trim().isEmpty()) {
                logger.warn("JSON de token está vazio");
                return null;
            }

            // Busca pelo campo access_token no JSON
            String searchKey = "\"access_token\":\"";
            int startIndex = tokenJson.indexOf(searchKey);

            if (startIndex == -1) {
                logger.warn("Campo 'access_token' não encontrado no JSON federado");
                logger.debugf("JSON structure: %s",
                        tokenJson.length() > 100 ? tokenJson.substring(0, 100) + "..." : tokenJson);
                return null;
            }

            // Posiciona após a chave
            startIndex += searchKey.length();

            // Busca o final do valor (próxima aspas não escapada)
            int endIndex = findJsonStringEnd(tokenJson, startIndex);

            if (endIndex == -1) {
                logger.warn("Formato inválido do access_token no JSON - aspas de fechamento não encontradas");
                return null;
            }

            String token = tokenJson.substring(startIndex, endIndex);

            // Validação básica do formato do token
            if (token.length() < 10) {
                logger.warn("Token extraído parece muito curto - possível erro de parsing");
                return null;
            }

            return token;

        } catch (Exception e) {
            logger.errorf("Erro ao fazer parsing do access_token do JSON: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Encontra o final de uma string JSON, tratando caracteres escapados
     */
    private static int findJsonStringEnd(String json, int startIndex) {
        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"') {
                // Verifica se não é uma aspas escapada
                if (i == startIndex || json.charAt(i - 1) != '\\') {
                    return i;
                }
            }
        }
        return -1;
    }
}