package br.com.spi.govbr.dto;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.forms.login.LoginFormsProvider;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.jboss.logging.Logger;
import static br.com.spi.govbr.config.GovBrConfig.LOGOUT_URL;
import static br.com.spi.govbr.config.GovBrConfig.ACCEPTED_LEVELS;
import java.util.Arrays;

public class GovBrThemeErrorHandler {

    private GovBrThemeErrorHandler() {}

    private static final Logger logger = Logger.getLogger(GovBrThemeErrorHandler.class);

    /**
     * Cria resposta de erro usando tema Keycloak com fallback
     * 
     * @param context Contexto de autenticação
     * @param errorType Tipo do erro (INSUFFICIENT_LEVEL, INVALID_TOKEN, etc.)
     * @param userLevel Nível atual do usuário (Bronze, Prata, Ouro)
     * @param customMessage Mensagem personalizada (opcional)
     * @return Response com página de erro
     */
    public static Response criarErroComTema(AuthenticationFlowContext context,
                                           String errorType,
                                           String userLevel,
                                           String customMessage) {
            return criarErroUsandoTema(context, errorType, userLevel, customMessage);
    }

    private static Response criarErroUsandoTema(AuthenticationFlowContext context,
                                              String errorType,
                                              String userLevel,
                                              String customMessage) {
        
        // Obter LoginFormsProvider e configurar contexto
        LoginFormsProvider forms = context.getSession().getProvider(LoginFormsProvider.class);

        // Passar dados para o template FreeMarker
        forms.setAttribute("errorType", errorType);
        forms.setAttribute("userLevel", userLevel);
        forms.setAttribute("errorMessage", customMessage);
        forms.setAttribute("loginUrl", construirUrlLogin(context));
        forms.setAttribute("logoutUrl", construirUrlLogout(context));
        forms.setAttribute("logoutGovBrUrl", construirUrlLogoutGovBr(context));
        
        // Informações dinâmicas sobre níveis aceitos
        forms.setAttribute("acceptedLevels", Arrays.asList(ACCEPTED_LEVELS));
        forms.setAttribute("acceptedLevelsText", gerarTextoNiveisAceitos());
        forms.setAttribute("isLevelAccepted", isNivelAceito(userLevel));

        // Renderiza o template govbr-error.ftl
        Response response = forms.createForm("govbr-error.ftl");
        
        logger.infof("✅ Página de erro Gov.br renderizada via tema - Tipo: %s, Nível: %s", 
                    errorType, userLevel);
        
        return response;
    }

    private static String construirUrlLogin(AuthenticationFlowContext context) {
        try {
            String baseUrl = obterBaseUrl(context);
            String realmName = context.getRealm().getName();
            var authSession = context.getAuthenticationSession();
            String clientId = authSession.getClient().getClientId();
            String redirectUri = authSession.getRedirectUri();
            String state = authSession.getClientNote("state");

            StringBuilder loginUrl = new StringBuilder();
            loginUrl.append(baseUrl)
                    .append("/realms/")
                    .append(realmName)
                    .append("/protocol/openid-connect/auth")
                    .append("?client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8))
                    .append("&response_type=code")
                    .append("&scope=openid")
                    .append("&prompt=login");

            if (redirectUri != null && !redirectUri.trim().isEmpty()) {
                loginUrl.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
            }

            if (state != null && !state.trim().isEmpty()) {
                loginUrl.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
            }

            String nonce = authSession.getClientNote("nonce");
            if (nonce != null && !nonce.trim().isEmpty()) {
                loginUrl.append("&nonce=").append(URLEncoder.encode(nonce, StandardCharsets.UTF_8));
            }

            String responseMode = authSession.getClientNote("response_mode");
            if (responseMode != null && !responseMode.trim().isEmpty()) {
                loginUrl.append("&response_mode=").append(URLEncoder.encode(responseMode, StandardCharsets.UTF_8));
            }

            return loginUrl.toString();

        } catch (Exception e) {
            logger.warnf("Erro ao construir URL de login: %s", e.getMessage());
            return obterBaseUrl(context) + "/realms/" + context.getRealm().getName() + "/protocol/openid-connect/auth?prompt=login";
        }
    }

    private static String construirUrlLogout(AuthenticationFlowContext context) {
        String baseUrl = obterBaseUrl(context);
        String realmName = context.getRealm().getName();
        return baseUrl + "/realms/" + realmName + "/protocol/openid-connect/logout";
    }

    private static String construirUrlLogoutGovBr(AuthenticationFlowContext context) {
        try {
            var authSession = context.getAuthenticationSession();
            String postLogoutRedirectUri = authSession.getRedirectUri();

            if (postLogoutRedirectUri != null && !postLogoutRedirectUri.trim().isEmpty()) {
                return LOGOUT_URL + "?post_logout_redirect_uri=" + 
                       URLEncoder.encode(postLogoutRedirectUri, StandardCharsets.UTF_8);
            }
            return LOGOUT_URL;

        } catch (Exception e) {
            logger.warnf("Erro ao construir URL de logout Gov.br: %s", e.getMessage());
            return LOGOUT_URL;
        }
    }

    private static String obterBaseUrl(AuthenticationFlowContext context) {
        String baseUrl = context.getSession().getContext().getUri().getBaseUri().toString();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    // ====== MÉTODOS DE CONVENIÊNCIA (TIPAGEM DE ERRO) ======

    public static Response erroNivelInsuficiente(AuthenticationFlowContext context, String nivelAtual) {
        return criarErroComTema(context, "INSUFFICIENT_LEVEL", nivelAtual, null);
    }

    public static Response erroTokenInvalido(AuthenticationFlowContext context) {
        return criarErroComTema(context, "INVALID_TOKEN", null, null);
    }

    public static Response erroServicoIndisponivel(AuthenticationFlowContext context) {
        return criarErroComTema(context, "SERVICE_UNAVAILABLE", null, null);
    }

    public static Response erroGenerico(AuthenticationFlowContext context, String mensagem) {
        return criarErroComTema(context, "GENERIC", null, mensagem);
    }

    // ====== MÉTODOS DINÂMICOS BASEADOS EM ACCEPTED_LEVELS ======

    /**
     * Verifica se o nível do usuário está na lista de níveis aceitos
     */
    private static boolean isNivelAceito(String userLevel) {
        if (userLevel == null) return false;
        return Arrays.stream(ACCEPTED_LEVELS)
                .anyMatch(level -> level.equalsIgnoreCase(userLevel.trim()));
    }

    /**
     * Gera texto descritivo dos níveis aceitos
     */
    private static String gerarTextoNiveisAceitos() {
        if (ACCEPTED_LEVELS.length == 0) {
            return "Nenhum nível configurado";
        }
        
        if (ACCEPTED_LEVELS.length == 1) {
            return "nível " + ACCEPTED_LEVELS[0];
        }
        
        if (ACCEPTED_LEVELS.length == 2) {
            return "níveis " + ACCEPTED_LEVELS[0] + " ou " + ACCEPTED_LEVELS[1];
        }
        
        // 3 ou mais níveis
        StringBuilder sb = new StringBuilder("níveis ");
        for (int i = 0; i < ACCEPTED_LEVELS.length - 1; i++) {
            sb.append(ACCEPTED_LEVELS[i]);
            if (i < ACCEPTED_LEVELS.length - 2) {
                sb.append(", ");
            }
        }
        sb.append(" ou ").append(ACCEPTED_LEVELS[ACCEPTED_LEVELS.length - 1]);
        return sb.toString();
    }

}