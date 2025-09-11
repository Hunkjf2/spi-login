package br.com.spi.govbr.util;

import br.com.spi.govbr.config.GovBrConfig;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.UserModel;

public class TokenExtractor {

    private static final Logger logger = Logger.getLogger(TokenExtractor.class);

    private TokenExtractor() {}

    public static String extrairTokenGovBr(AuthenticationFlowContext context) {
        try {
            UserModel user = context.getUser();
            if (user == null) {
                return null;
            }

            String tokenJson = context.getSession().users()
                    .getFederatedIdentitiesStream(context.getRealm(), user)
                    .filter(fed -> GovBrConfig.PROVIDER_ALIAS.equals(fed.getIdentityProvider()))
                    .findFirst()
                    .map(FederatedIdentityModel::getToken)
                    .orElse(null);

            return extrairAccessToken(tokenJson);

        } catch (Exception e) {
            logger.errorf("Erro ao extrair token Gov.br: %s", e.getMessage());
            return null;
        }
    }

    private static String extrairAccessToken(String tokenJson) {
        if (tokenJson == null || tokenJson.trim().isEmpty()) {
            return null;
        }

        try {
            String searchKey = "\"access_token\":\"";
            int startIndex = tokenJson.indexOf(searchKey);

            if (startIndex == -1) {
                return null;
            }

            startIndex += searchKey.length();
            int endIndex = tokenJson.indexOf("\"", startIndex);

            if (endIndex == -1) {
                return null;
            }

            return tokenJson.substring(startIndex, endIndex);

        } catch (Exception e) {
            logger.errorf("Erro ao fazer parsing do token: %s", e.getMessage());
            return null;
        }
    }
}