package br.com.spi.govbr.util;

import br.com.spi.govbr.config.GovBrValidatorConfig;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.UserModel;

public class TokenExtractor {

    private static final Logger logger = Logger.getLogger(TokenExtractor.class);

    private TokenExtractor() {}

    /**
     * Extrai o token de acesso Gov.br do contexto de autenticação
     */
    public static String extrairTokenGovBr(AuthenticationFlowContext context) {
        try {
            UserModel user = context.getUser();
            if (user == null) {
                logger.warn("Usuário não encontrado no contexto");
                return null;
            }

            String tokenJson = context.getSession().users()
                    .getFederatedIdentitiesStream(context.getRealm(), user)
                    .filter(fed -> GovBrValidatorConfig.getGovBrProviderAlias()
                            .equals(fed.getIdentityProvider()))
                    .findFirst()
                    .map(FederatedIdentityModel::getToken)
                    .orElse(null);

            if (tokenJson == null) {
                logger.warn("Token federado Gov.br não encontrado");
                return null;
            }

            return extrairAccessToken(tokenJson);

        } catch (Exception e) {
            logger.errorf("Erro ao extrair token Gov.br: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Extrai o access_token do JSON de token federado
     */
    private static String extrairAccessToken(String tokenJson) {
        try {
            String searchKey = "\"access_token\":\"";
            int startIndex = tokenJson.indexOf(searchKey);

            if (startIndex == -1) {
                logger.warn("Campo access_token não encontrado no JSON");
                return null;
            }

            startIndex += searchKey.length();
            int endIndex = tokenJson.indexOf("\"", startIndex);

            if (endIndex == -1) {
                logger.warn("Formato inválido do access_token no JSON");
                return null;
            }

            return tokenJson.substring(startIndex, endIndex);

        } catch (Exception e) {
            logger.errorf("Erro ao extrair access_token: %s", e.getMessage());
            return null;
        }
    }
}