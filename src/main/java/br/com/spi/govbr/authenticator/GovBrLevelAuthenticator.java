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
import jakarta.ws.rs.core.Response;

/**
 * Authenticator que valida o nível de autenticação Gov.br durante o fluxo de login
 */
public class GovBrLevelAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(GovBrLevelAuthenticator.class);

    private final LevelValidationService validationService;

    public GovBrLevelAuthenticator() {
        this.validationService = new LevelValidationService();
        logger.debug("GovBrLevelAuthenticator instanciado");
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        logger.infof("=== Iniciando validação de nível Gov.br ===");

        try {
            // Verifica se é necessário validar este login
            if (!deveValidarLogin(context)) {
                context.success();
                return;
            }

            // Extrai token de acesso Gov.br
            String accessToken = TokenExtractor.extrairTokenGovBr(context);
            if (accessToken == null) {
                logger.error("Token Gov.br não encontrado no contexto de autenticação");
                redirecionarParaLoginComErro(context, "Token de autenticação Gov.br não encontrado");
                return;
            }

            // Executa validação de nível
            ValidationResult result = validationService.validarNivelUsuario(accessToken);

            // Processa resultado da validação
            processarResultadoValidacao(context, result);

        } catch (Exception e) {
            logger.errorf("Erro inesperado durante validação Gov.br: %s", e.getMessage());
            redirecionarParaLoginComErro(context, "Erro interno durante validação. Tente novamente.");
        }
    }

    /**
     * Determina se deve validar este login baseado no contexto
     */
    private boolean deveValidarLogin(AuthenticationFlowContext context) {

        // Verifica se há usuário no contexto
        UserModel user = context.getUser();
        if (user == null) {
            logger.warn("Usuário não encontrado no contexto - pulando validação");
            return false;
        }

        // Verifica se é login via Gov.br
        boolean isGovBrLogin = isLoginViaGovBr(context, user);
        if (!isGovBrLogin) {
            logger.infof("Login do usuário %s não é via Gov.br - pulando validação",
                    user.getUsername());
            return false;
        }

        logger.infof("Login via Gov.br detectado para usuário %s - validação necessária",
                user.getUsername());
        return true;
    }

    /**
     * Verifica se o login foi realizado via provedor Gov.br
     */
    private boolean isLoginViaGovBr(AuthenticationFlowContext context, UserModel user) {
        try {
            return context.getSession().users()
                    .getFederatedIdentitiesStream(context.getRealm(), user)
                    .anyMatch(fed -> GovBrValidatorConfig.getGovBrProviderAlias()
                            .equals(fed.getIdentityProvider()));

        } catch (Exception e) {
            logger.errorf("Erro ao verificar provedor Gov.br para usuário %s: %s",
                    user.getUsername(), e.getMessage());
            return false;
        }
    }

    private void falharAutenticacao(AuthenticationFlowContext context, String errorMessage, AuthenticationFlowError errorType) {
        logger.warnf("Falhando autenticação: %s", errorMessage);

        // Limpa o usuário do contexto
        context.clearUser();

        // Cria response com mensagem de erro
        Response response = context.form()
                .setError(errorMessage)
                .createErrorPage(Response.Status.UNAUTHORIZED);

        // Falha o contexto com o tipo de erro especificado
        context.failure(errorType, response);
    }

    /**
     * Processa o resultado da validação de nível
     */
    private void processarResultadoValidacao(AuthenticationFlowContext context, ValidationResult result) {

        UserModel user = context.getUser();
        String username = user != null ? user.getUsername() : "unknown";

        if (result.isValid()) {
            logger.infof("✅ Validação APROVADA para usuário %s - Nível: %s",
                    username, result.userLevel());

            // Salva o nível como atributo do usuário
            salvarNivelComoAtributo(user, result.userLevel());

            context.success();

        } else {
            String errorMsg = result.errorMessage() != null ?
                    result.errorMessage() : "Nível de autenticação insuficiente";

            logger.warnf("❌ Validação REJEITADA para usuário %s - %s", username, errorMsg);

            // Determina tipo de erro baseado no resultado
            AuthenticationFlowError errorType = result.hasApiError() ?
                    AuthenticationFlowError.INTERNAL_ERROR :
                    AuthenticationFlowError.INVALID_CREDENTIALS;

            falharAutenticacao(context, errorMsg, errorType);
        }
    }

    /**
     * Salva o nível validado como atributo do usuário para referência futura
     */
    private void salvarNivelComoAtributo(UserModel user, String nivel) {
        try {
            if (user != null && nivel != null) {
                user.setSingleAttribute("govbr_nivel", nivel);
                user.setSingleAttribute("govbr_nivel_validated_at",
                        String.valueOf(System.currentTimeMillis()));
                logger.debugf("Nível %s salvo como atributo do usuário %s", nivel, user.getUsername());
            }
        } catch (Exception e) {
            logger.warnf("Erro ao salvar nível como atributo: %s", e.getMessage());
        }
    }

    /**
     * Redireciona o usuário de volta para a tela de login com mensagem de erro
     */
    private void redirecionarParaLoginComErro(AuthenticationFlowContext context, String errorMessage) {

        logger.warnf("Redirecionando para login com erro: %s", errorMessage);

        // Remove o usuário do contexto para forçar novo login
        context.clearUser();

        // Limpa qualquer sessão federada existente para evitar loop
        try {
            if (context.getAuthenticationSession() != null) {
                context.getAuthenticationSession().removeAuthNote("FEDERATED_ACCESS_TOKEN");
                context.getAuthenticationSession().removeAuthNote("IDENTITY_PROVIDER_IDENTITY");
            }
        } catch (Exception e) {
            logger.warnf("Erro ao limpar notas da sessão: %s", e.getMessage());
        }

        // Cria response com redirecionamento para login e mensagem de erro
        Response response = context.form()
                .setError(errorMessage)
                .createErrorPage(Response.Status.UNAUTHORIZED);

        // Força falha que resulta em redirecionamento para login
        context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, response);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Este authenticator não processa ações do usuário
        logger.debug("Método action() chamado - nenhuma ação necessária");
    }

    @Override
    public boolean requiresUser() {
        return true; // Requer usuário autenticado para funcionar
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // Sempre considerado configurado
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Não define ações requeridas adicionais
    }

    @Override
    public void close() {
        // Cleanup de recursos se necessário
        logger.debug("GovBrLevelAuthenticator fechado");
    }
}