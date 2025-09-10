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
                redirecionarParaLogin(context, "Token de autenticação Gov.br não encontrado");
                return;
            }

            // Executa validação de nível
            ValidationResult result = validationService.validarNivelUsuario(accessToken);

            // Processa resultado da validação
            processarResultadoValidacao(context, result);

        } catch (Exception e) {
            logger.errorf("Erro inesperado durante validação Gov.br: %s", e.getMessage());
            redirecionarParaLogin(context, "Erro interno durante validação. Tente novamente.");
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

            context.success();

        } else {
            String errorMsg = result.errorMessage() != null ?
                    result.errorMessage() : "Nível de autenticação insuficiente";

            logger.warnf("❌ Validação REJEITADA para usuário %s - %s", username, errorMsg);

            // Redireciona para login sem remover dados permanentes
            redirecionarParaLogin(context, errorMsg);
        }
    }

    /**
     * Limpa a sessão atual completamente e força nova autenticação
     * NÃO remove o usuário nem vínculos federados permanentes
     */
    private void redirecionarParaLogin(AuthenticationFlowContext context, String errorMessage) {

        logger.warnf("Redirecionando para login devido a: %s", errorMessage);

        try {
            UserModel user = context.getUser();

            // 1. Remove TODAS as sessões ativas do usuário para forçar novo login
            if (user != null) {
                logger.infof("Removendo todas as sessões ativas do usuário: %s", user.getUsername());

                context.getSession().sessions()
                        .getUserSessionsStream(context.getRealm(), user)
                        .forEach(userSession -> {
                            logger.debugf("Removendo sessão de usuário: %s", userSession.getId());
                            context.getSession().sessions().removeUserSession(context.getRealm(), userSession);
                        });
            }

            // 2. Limpa completamente a sessão de autenticação atual
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            if (authSession != null) {
                logger.debug("Limpando TODOS os dados da sessão de autenticação");

                String[] notesToRemove = {
                        "BROKER_NONCE", "BROKER_STATE", "BROKER_USERNAME",
                        "BROKER_SESSION_ID", "BROKER_PROVIDER_ID",
                        "IDENTITY_PROVIDER_IDENTITY", "BROKERED_CONTEXT_NOTE",
                        "FEDERATED_ACCESS_TOKEN"
                };

                for (String note : notesToRemove) {
                    authSession.removeAuthNote(note);
                }

                // Remove notas de sessão do usuário
                authSession.getUserSessionNotes().clear();

                // Remove dados do cliente
                authSession.getClientNotes().clear();
            }

            logger.info("Todas as sessões foram limpas - criando resposta de redirecionamento");

            // 3. Cria resposta de redirecionamento direto para página de login limpa
            String loginUrl = construirUrlLogin(context);

            Response redirectResponse = Response.status(Response.Status.FOUND)
                    .location(URI.create(loginUrl))
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .header("Clear-Site-Data", "\"cache\", \"cookies\", \"storage\"")
                    .build();

            logger.infof("Redirecionando para: %s", loginUrl);

            // Usa GENERIC_AUTHENTICATION_ERROR com resposta de redirecionamento
            context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR, redirectResponse);

        } catch (Exception e) {
            logger.errorf("Erro durante redirecionamento: %s", e.getMessage());

            // Fallback: apenas falha simples
            context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR);
        }
    }

    /**
     * Constrói URL de login do realm com todos os parâmetros necessários
     */
    private String construirUrlLogin(AuthenticationFlowContext context) {
        try {
            String realmName = context.getRealm().getName();
            String baseUrl = context.getSession().getContext().getUri().getBaseUri().toString();

            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            // Extrai informações da sessão de autenticação atual
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            String clientId = authSession.getClient().getClientId();
            String redirectUri = authSession.getRedirectUri();
            String state = authSession.getClientNote("state");
            String nonce = authSession.getClientNote("nonce");
            String responseMode = authSession.getClientNote("response_mode");
            String responseType = authSession.getClientNote("response_type");
            String scope = authSession.getClientNote("scope");

            // Constrói a URL de autorização completa
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(String.format("%s/realms/%s/protocol/openid-connect/auth", baseUrl, realmName));
            urlBuilder.append("?client_id=").append(java.net.URLEncoder.encode(clientId, "UTF-8"));

            if (redirectUri != null) {
                urlBuilder.append("&redirect_uri=").append(java.net.URLEncoder.encode(redirectUri, "UTF-8"));
            }

            if (state != null) {
                urlBuilder.append("&state=").append(java.net.URLEncoder.encode(state, "UTF-8"));
            }

            if (responseMode != null) {
                urlBuilder.append("&response_mode=").append(java.net.URLEncoder.encode(responseMode, "UTF-8"));
            } else {
                urlBuilder.append("&response_mode=fragment");
            }

            if (responseType != null) {
                urlBuilder.append("&response_type=").append(java.net.URLEncoder.encode(responseType, "UTF-8"));
            } else {
                urlBuilder.append("&response_type=code");
            }

            if (scope != null) {
                urlBuilder.append("&scope=").append(java.net.URLEncoder.encode(scope, "UTF-8"));
            } else {
                urlBuilder.append("&scope=openid");
            }

            if (nonce != null) {
                urlBuilder.append("&nonce=").append(java.net.URLEncoder.encode(nonce, "UTF-8"));
            }

            String finalUrl = urlBuilder.toString();
            logger.infof("URL de login construída: %s", finalUrl);

            return finalUrl;

        } catch (Exception e) {
            logger.errorf("Erro ao construir URL de login: %s", e.getMessage());

            // Fallback com client_id fixo
            return "http://localhost:8080/realms/estudos/protocol/openid-connect/auth?client_id=teste-frontend&response_type=code&scope=openid";
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