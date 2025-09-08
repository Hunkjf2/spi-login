package br.com.spi.govbr.authenticator;

import br.com.spi.govbr.config.GovBrValidatorConfig;
import br.com.spi.govbr.dto.ValidationResult;
import br.com.spi.govbr.service.LevelValidationService;
import br.com.spi.govbr.util.TokenExtractor;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.Response;

public class GovBrLevelAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(GovBrLevelAuthenticator.class);
    private final LevelValidationService validationService;

    public GovBrLevelAuthenticator() {
        this.validationService = new LevelValidationService();
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("=== Iniciando validação de nível Gov.br ===");

        // Verifica se é login via Gov.br
        if (!isGovBrLogin(context)) {
            logger.info("Login não é via Gov.br, saltando validação");
            context.success();
            return;
        }

        // Extrai token de acesso
        String accessToken = TokenExtractor.extrairTokenGovBr(context);
        if (accessToken == null) {
            logger.error("Token Gov.br não encontrado");
            forceError(context, "Token de autenticação Gov.br não encontrado");
            return;
        }

        // Valida nível do usuário
        ValidationResult result = validationService.validarNivelUsuario(accessToken);

        if (result.isValid()) {
            logger.infof("Validação bem-sucedida. Nível: %s", result.userLevel());
            context.success();
        } else {
            logger.warnf("Validação falhou: %s", result.errorMessage());
            forceError(context, result.errorMessage());
        }
    }

    /**
     * Verifica se o login foi realizado via provedor Gov.br
     */
    private boolean isGovBrLogin(AuthenticationFlowContext context) {
        try {
            UserModel user = context.getUser();
            if (user == null) return false;

            return context.getSession().users()
                    .getFederatedIdentitiesStream(context.getRealm(), user)
                    .anyMatch(fed -> GovBrValidatorConfig.getGovBrProviderAlias()
                            .equals(fed.getIdentityProvider()));

        } catch (Exception e) {
            logger.errorf("Erro ao verificar provedor Gov.br: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Força erro de autenticação com mensagem customizada
     */
    private void forceError(AuthenticationFlowContext context, String errorMessage) {
        Response response = context.form()
                .setError(errorMessage)
                .createErrorPage(Response.Status.UNAUTHORIZED);

        context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, response);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Não implementado - este authenticator não processa ações
    }

    @Override
    public boolean requiresUser() {
        return true; // Requer usuário autenticado
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true; // Sempre configurado
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Não há ações requeridas
    }

    @Override
    public void close() {
        // Cleanup se necessário
    }
}