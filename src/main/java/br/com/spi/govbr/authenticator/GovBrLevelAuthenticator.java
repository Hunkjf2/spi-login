package br.com.spi.govbr.authenticator;

import br.com.spi.govbr.config.GovBrConfig;
import br.com.spi.govbr.dto.ValidationResult;
import br.com.spi.govbr.dto.GovBrErrorResponseHandler;
import br.com.spi.govbr.service.LevelValidationService;
import br.com.spi.govbr.util.GovBrSessionCleaner;
import br.com.spi.govbr.util.TokenExtractor;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import jakarta.ws.rs.core.Response;

public class GovBrLevelAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(GovBrLevelAuthenticator.class);

    private final LevelValidationService validationService;

    public GovBrLevelAuthenticator() {
        this.validationService = new LevelValidationService();
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        logger.info("=== Iniciando validação Gov.br ===");

        try {
            // Verifica se deve validar o login
            if (!deveValidarLogin(context)) {
                context.success();
                return;
            }

            // Extrai o token de acesso
            String accessToken = TokenExtractor.extrairTokenGovBr(context);
            if (accessToken == null) {
                logger.warn("Token Gov.br não encontrado na sessão");
                exibirErroTokenInvalido(context);
                return;
            }

            // Valida o nível do usuário
            ValidationResult result = validationService.validarNivelUsuario(accessToken);

            if (result.isValid()) {
                logger.infof("✅ Login aprovado - Usuário: %s - Nível: %s",
                        context.getUser().getUsername(), result.userLevel());
                context.success();
            } else {
                logger.warnf("❌ Login rejeitado - Usuário: %s - Erro: %s",
                        context.getUser().getUsername(), result.errorMessage());
                exibirErroBaseadoNoResultado(context, result);
            }

        } catch (Exception e) {
            logger.errorf("Erro inesperado na validação Gov.br: %s", e.getMessage());
            exibirErroServicoIndisponivel(context);
        }
    }

    private boolean deveValidarLogin(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            logger.debug("Usuário não encontrado no contexto");
            return false;
        }

        boolean isGovBrLogin = context.getSession().users()
                .getFederatedIdentitiesStream(context.getRealm(), user)
                .anyMatch(fed -> GovBrConfig.PROVIDER_ALIAS.equals(fed.getIdentityProvider()));

        if (isGovBrLogin) {
            logger.infof("Login Gov.br detectado para usuário: %s", user.getUsername());
        } else {
            logger.debug("Login não é via Gov.br, pulando validação");
        }

        return isGovBrLogin;
    }

    private void exibirErroBaseadoNoResultado(AuthenticationFlowContext context,
                                              ValidationResult result) {
        // Limpa sessões antes de exibir erro
        GovBrSessionCleaner.limparSessoesUsuario(context);

        Response errorResponse;

        if (result.errorMessage().contains("Token")) {
            errorResponse = GovBrErrorResponseHandler.erroTokenInvalido(context);
        } else if (result.errorMessage().contains("insuficiente")) {
            errorResponse = GovBrErrorResponseHandler.erroNivelInsuficiente(
                    context, result.userLevel());
        } else if (result.errorMessage().contains("indisponível")) {
            errorResponse = GovBrErrorResponseHandler.erroServicoIndisponivel(context);
        } else {
            // Erro genérico
            errorResponse = GovBrErrorResponseHandler.criarPaginaErro(
                    context,
                    "Erro de Validação",
                    result.errorMessage(),
                    "Ocorreu um erro durante a validação do seu nível Gov.br."
            );
        }

        context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, errorResponse);
    }

    private void exibirErroTokenInvalido(AuthenticationFlowContext context) {
        // Limpa sessões antes de exibir erro
        GovBrSessionCleaner.limparSessoesUsuario(context);

        Response errorResponse = GovBrErrorResponseHandler.erroTokenInvalido(context);
        context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, errorResponse);
    }

    private void exibirErroServicoIndisponivel(AuthenticationFlowContext context) {
        // Limpa sessões antes de exibir erro
        GovBrSessionCleaner.limparSessoesUsuario(context);

        Response errorResponse = GovBrErrorResponseHandler.erroServicoIndisponivel(context);
        context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR, errorResponse);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Não utilizado neste authenticator
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Não utilizado
    }

    @Override
    public void close() {
        // Cleanup se necessário
    }
}