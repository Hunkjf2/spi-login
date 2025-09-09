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
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import jakarta.ws.rs.core.Response;
import java.net.URI;

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
                encerrarSessaoERedirecionarLogin(context, "Token de autenticação Gov.br não encontrado");
                return;
            }

            // Executa validação de nível
            ValidationResult result = validationService.validarNivelUsuario(accessToken);

            // Processa resultado da validação
            processarResultadoValidacao(context, result);

        } catch (Exception e) {
            logger.errorf("Erro inesperado durante validação Gov.br: %s", e.getMessage());
            encerrarSessaoERedirecionarLogin(context, "Erro interno durante validação. Tente novamente.");
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

            // Encerra sessão completamente e redireciona para login
            encerrarSessaoERedirecionarLogin(context, errorMsg);
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
     * Encerra completamente a sessão do usuário e redireciona para login
     */
    private void encerrarSessaoERedirecionarLogin(AuthenticationFlowContext context, String errorMessage) {

        logger.warnf("Encerrando sessão completamente devido a: %s", errorMessage);

        UserModel user = context.getUser();

        try {
            // 1. Remove todas as sessões ativas do usuário
            if (user != null) {
                logger.infof("Removendo todas as sessões do usuário: %s", user.getUsername());

                context.getSession().sessions()
                        .getUserSessionsStream(context.getRealm(), user)
                        .forEach(userSession -> {
                            logger.debugf("Removendo sessão: %s", userSession.getId());
                            context.getSession().sessions().removeUserSession(context.getRealm(), userSession);
                        });

//                // 2. Remove identidades federadas temporariamente
//                context.getSession().users()
//                        .getFederatedIdentitiesStream(context.getRealm(), user)
//                        .filter(fed -> GovBrValidatorConfig.getGovBrProviderAlias().equals(fed.getIdentityProvider()))
//                        .forEach(fed -> {
//                            logger.debugf("Removendo identidade federada Gov.br do usuário: %s", user.getUsername());
//                            context.getSession().users().removeFederatedIdentity(context.getRealm(), user, fed.getIdentityProvider());
//                        });
            }

            // 3. Limpa sessão de autenticação atual
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            if (authSession != null) {
                logger.debug("Limpando dados da sessão de autenticação");

                // Remove notas específicas conhecidas (ajuste conforme necessário)
                authSession.removeAuthNote("BROKER_NONCE");
                authSession.removeAuthNote("BROKER_STATE");
                authSession.removeAuthNote("BROKER_USERNAME");

                // Limpa notas da sessão do usuário
                authSession.getUserSessionNotes().clear();
            }

            // 4. Remove usuário do contexto
            context.clearUser();

            logger.info("Sessão encerrada com sucesso - redirecionando para logout");

        } catch (Exception e) {
            logger.errorf("Erro ao encerrar sessão: %s", e.getMessage());
        }

        // 5. Cria redirecionamento direto para logout
        try {
            String logoutUrl = construirUrlLogout(context);

            Response redirectResponse = Response.status(Response.Status.FOUND)
                    .location(URI.create(logoutUrl))
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .build();

            context.failure(AuthenticationFlowError.INVALID_USER, redirectResponse);

        } catch (Exception e) {
            logger.errorf("Erro ao criar redirecionamento de logout: %s", e.getMessage());

            // Fallback: página simples com link de logout
            criarPaginaLogout(context, errorMessage);
        }
    }

    /**
     * Constrói URL de logout completo do Keycloak
     */
    private String construirUrlLogout(AuthenticationFlowContext context) {
        String realmName = context.getRealm().getName();
        String baseUrl = context.getSession().getContext().getUri().getBaseUri().toString();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // URL de logout simples sem parâmetros problemáticos
        return String.format("%s/realms/%s/protocol/openid-connect/logout", baseUrl, realmName);
    }

    /**
     * Cria página simples de logout como fallback
     */
    private void criarPaginaLogout(AuthenticationFlowContext context, String errorMessage) {
        try {
            String logoutUrl = construirUrlLogout(context);

            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>Sessão Encerrada</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }
                        .container { background: white; padding: 40px; border-radius: 8px; max-width: 600px; margin: 0 auto; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .error { color: #d32f2f; margin-bottom: 20px; }
                        .message { margin-bottom: 30px; line-height: 1.6; }
                        .button { background: #1976d2; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h2 class="error">Sessão Encerrada</h2>
                        <div class="message">%s</div>
                        <p>Sua sessão foi encerrada. Clique no botão abaixo para fazer logout completo.</p>
                        <a href="%s" class="button">Fazer Logout</a>
                    </div>
                    <script>
                        // Redireciona automaticamente após 2 segundos
                        setTimeout(function() {
                            window.location.href = '%s';
                        }, 2000);
                    </script>
                </body>
                </html>
                """, errorMessage, logoutUrl, logoutUrl);

            Response response = Response.status(Response.Status.UNAUTHORIZED)
                    .entity(html)
                    .type("text/html; charset=utf-8")
                    .build();

            context.failure(AuthenticationFlowError.INVALID_USER, response);

        } catch (Exception e) {
            logger.errorf("Erro ao criar página de logout: %s", e.getMessage());
            context.failure(AuthenticationFlowError.INVALID_USER);
        }
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