package br.com.spi.govbr.authenticator;

import br.com.spi.govbr.config.GovBrConfig;
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
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
            if (!deveValidarLogin(context)) {
                context.success();
                return;
            }

            String accessToken = TokenExtractor.extrairTokenGovBr(context);
            if (accessToken == null) {
                executarLogout(context);
                return;
            }

            ValidationResult result = validationService.validarNivelUsuario(accessToken);

            if (result.isValid()) {
                logger.infof("✅ Login aprovado - Nível: %s", result.userLevel());
                context.success();
            } else {
                logger.warnf("❌ Login rejeitado - %s", result.errorMessage());
                executarLogout(context);
            }

        } catch (Exception e) {
            logger.errorf("Erro na validação: %s", e.getMessage());
            executarLogout(context);
        }
    }

    private boolean deveValidarLogin(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            return false;
        }

        boolean isGovBrLogin = context.getSession().users()
                .getFederatedIdentitiesStream(context.getRealm(), user)
                .anyMatch(fed -> GovBrConfig.PROVIDER_ALIAS.equals(fed.getIdentityProvider()));

        if (isGovBrLogin) {
            logger.infof("Login Gov.br detectado para: %s", user.getUsername());
        }

        return isGovBrLogin;
    }

    private void executarLogout(AuthenticationFlowContext context) {

        logger.warn("Executando logout completo");

        try {
            // Remove sessões do usuário
            UserModel user = context.getUser();
            if (user != null) {
                context.getSession().sessions()
                        .getUserSessionsStream(context.getRealm(), user)
                        .forEach(session -> context.getSession().sessions()
                                .removeUserSession(context.getRealm(), session));
            }

            // Limpa sessão de autenticação
            var authSession = context.getAuthenticationSession();
            if (authSession != null) {
                authSession.getUserSessionNotes().clear();
                authSession.getClientNotes().clear();
                authSession.removeAuthNote("FEDERATED_ACCESS_TOKEN");
            }

            // Constrói URL de logout Gov.br com redirecionamento para login
            String logoutUrl = construirUrlLogoutComRedirect(context);

            logger.infof("Redirecionando para logout Gov.br: %s", logoutUrl);

            // Redireciona para logout Gov.br (que depois volta para login)
            Response response = Response.status(302)
                    .location(URI.create(logoutUrl))
                    .header("Cache-Control", "no-cache")
                    .build();

            context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR, response);

        } catch (Exception e) {
            logger.errorf("Erro no logout: %s", e.getMessage());
            context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR);
        }
    }

    private String construirUrlLogoutComRedirect(AuthenticationFlowContext context) {
        try {
            String realmName = context.getRealm().getName();
            String baseUrl = context.getSession().getContext().getUri().getBaseUri().toString();

            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            // Extrai TODOS os parâmetros da sessão atual
            var authSession = context.getAuthenticationSession();
            String clientId = authSession.getClient().getClientId();
            String redirectUri = authSession.getRedirectUri();
            String state = authSession.getClientNote("state");
            String responseMode = authSession.getClientNote("response_mode");
            String responseType = authSession.getClientNote("response_type");
            String scope = authSession.getClientNote("scope");
            String nonce = authSession.getClientNote("nonce");

            // Constrói URL de login completa com TODOS os parâmetros
            StringBuilder loginUrl = new StringBuilder();
            loginUrl.append(baseUrl)
                    .append("/realms/")
                    .append(realmName)
                    .append("/protocol/openid-connect/auth")
                    .append("?client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));

            if (redirectUri != null && !redirectUri.trim().isEmpty()) {
                loginUrl.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
            }

            if (state != null && !state.trim().isEmpty()) {
                loginUrl.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
            }

            if (responseMode != null && !responseMode.trim().isEmpty()) {
                loginUrl.append("&response_mode=").append(URLEncoder.encode(responseMode, StandardCharsets.UTF_8));
            }

            if (responseType != null && !responseType.trim().isEmpty()) {
                loginUrl.append("&response_type=").append(URLEncoder.encode(responseType, StandardCharsets.UTF_8));
            } else {
                loginUrl.append("&response_type=code");
            }

            if (scope != null && !scope.trim().isEmpty()) {
                loginUrl.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
            } else {
                loginUrl.append("&scope=openid");
            }

            if (nonce != null && !nonce.trim().isEmpty()) {
                loginUrl.append("&nonce=").append(URLEncoder.encode(nonce, StandardCharsets.UTF_8));
            }

            String finalLoginUrl = loginUrl.toString();
            logger.infof("URL de login completa construída: %s", finalLoginUrl);

            String encodedLoginUrl = URLEncoder.encode(finalLoginUrl, StandardCharsets.UTF_8);

            // URL completa: logout Gov.br + redirecionamento para login
            String logoutUrl = String.format("%s?post_logout_redirect_uri=%s",
                    GovBrConfig.LOGOUT_URL, encodedLoginUrl);

            logger.infof("URL logout Gov.br construída: %s", logoutUrl);
            return logoutUrl;

        } catch (Exception e) {
            logger.errorf("Erro ao construir URL logout: %s", e.getMessage());

            // Fallback: logout simples sem redirecionamento
            return GovBrConfig.LOGOUT_URL;
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Não utilizado
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