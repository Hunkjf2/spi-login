package br.com.spi.govbr.dto;

import org.keycloak.authentication.AuthenticationFlowContext;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static br.com.spi.govbr.config.GovBrConfig.LOGOUT_URL;

/**
 * Gerador de páginas de erro HTML para o SPI Gov.br
 */
public class GovBrErrorResponseHandler {

    /**
     * Gera uma resposta de erro com página HTML personalizada
     */
    public static Response criarPaginaErro(AuthenticationFlowContext context,
                                           String titulo,
                                           String mensagem,
                                           String detalhes) {
        String htmlContent = gerarHtmlErro(context, titulo, mensagem, detalhes);

        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(htmlContent)
                .type("text/html; charset=UTF-8")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build();
    }

    private static String gerarHtmlErro(AuthenticationFlowContext context,
                                        String titulo,
                                        String mensagem,
                                        String detalhes) {

        String baseUrl = obterBaseUrl(context);
        String loginUrl = construirUrlLogin(context, baseUrl);
        String logoutUrl = construirUrlLogout(baseUrl, context.getRealm().getName());
        String logoutGovBrUrl = construirUrlLogoutGovBr(context);

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s - Gov.br</title>
                <style>
                    * {
                        box-sizing: border-box;
                        margin: 0;
                        padding: 0;
                    }

                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: linear-gradient(135deg, #1e3c72, #2a5298);
                        color: #333;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }

                    .error-container {
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                        padding: 40px;
                        max-width: 500px;
                        width: 100%%;
                        text-align: center;
                        position: relative;
                        overflow: hidden;
                    }

                    .error-container::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        height: 5px;
                        background: linear-gradient(90deg, #ff6b6b, #ffa726, #42a5f5);
                    }

                    .error-icon {
                        font-size: 4rem;
                        color: #ff6b6b;
                        margin-bottom: 20px;
                        display: block;
                    }

                    .error-title {
                        font-size: 1.8rem;
                        color: #2c3e50;
                        margin-bottom: 15px;
                        font-weight: 600;
                    }

                    .error-message {
                        font-size: 1.1rem;
                        color: #555;
                        margin-bottom: 20px;
                        line-height: 1.5;
                    }

                    .error-details {
                        background: #f8f9fa;
                        border-left: 4px solid #ffa726;
                        padding: 15px;
                        margin: 20px 0;
                        text-align: left;
                        font-size: 0.95rem;
                        color: #666;
                        border-radius: 4px;
                    }

                    .button-container {
                        margin-top: 30px;
                        display: flex;
                        gap: 15px;
                        justify-content: center;
                        flex-wrap: wrap;
                    }

                    .btn {
                        padding: 12px 24px;
                        border: none;
                        border-radius: 6px;
                        font-size: 1rem;
                        font-weight: 500;
                        cursor: pointer;
                        text-decoration: none;
                        display: inline-flex;
                        align-items: center;
                        gap: 8px;
                        transition: all 0.3s ease;
                        min-width: 140px;
                        justify-content: center;
                    }

                    .btn-primary {
                        background: #2196f3;
                        color: white;
                    }

                    .btn-primary:hover {
                        background: #1976d2;
                        transform: translateY(-2px);
                        box-shadow: 0 5px 15px rgba(33,150,243,0.3);
                    }

                    .govbr-info {
                        margin-top: 30px;
                        padding: 20px;
                        background: #e8f4f8;
                        border-radius: 8px;
                        border: 1px solid #b3d9e6;
                    }

                    .govbr-info h3 {
                        color: #1976d2;
                        margin-bottom: 10px;
                        font-size: 1.2rem;
                    }

                    .govbr-info p {
                        color: #555;
                        font-size: 0.95rem;
                        line-height: 1.4;
                        margin-bottom: 8px;
                    }

                    .govbr-info a {
                        color: #1976d2;
                        text-decoration: none;
                        font-weight: 500;
                    }

                    .govbr-info a:hover {
                        text-decoration: underline;
                    }

                    @media (max-width: 480px) {
                        .error-container {
                            padding: 30px 20px;
                        }

                        .error-title {
                            font-size: 1.5rem;
                        }

                        .button-container {
                            flex-direction: column;
                            align-items: stretch;
                        }

                        .btn {
                            width: 100%%;
                        }
                    }

                    .loading {
                        display: none;
                        margin-left: 10px;
                    }

                    .loading.show {
                        display: inline-block;
                        width: 16px;
                        height: 16px;
                        border: 2px solid #ffffff40;
                        border-top: 2px solid #ffffff;
                        border-radius: 50%%;
                        animation: spin 1s linear infinite;
                    }

                    @keyframes spin {
                        0%% { transform: rotate(0deg); }
                        100%% { transform: rotate(360deg); }
                    }
                </style>
            </head>
            <body>
                <div class="error-container">
                    <div class="error-icon">🚫</div>
                    <h1 class="error-title">%s</h1>
                    <p class="error-message">%s</p>

                    %s

                    <div class="button-container">
                        <a href="%s" class="btn btn-primary" onclick="showLoading(this)">
                            🔄 Tentar Novamente
                            <span class="loading"></span>
                        </a>
                    </div>
                    <div class="govbr-info">
                        <h3>📋 Sobre os Níveis Gov.br</h3>
                        <p><strong>Bronze:</strong> Cadastro básico com CPF</p>
                        <p><strong>Prata:</strong> Validação de dados bancários</p>
                        <p><strong>Ouro:</strong> Validação presencial ou certificado digital</p>
                        <p>
                            <a href="https://www.gov.br/governodigital/pt-br/seguranca-e-protecao-de-dados/como-aumentar-o-nivel-da-conta-gov-br"
                               target="_blank">
                                📈 Como aumentar seu nível?
                            </a>
                        </p>
                    </div>
                </div>

                <script>
                    function showLoading(button) {
                        const loading = button.querySelector('.loading');
                        if (loading) {
                            loading.classList.add('show');
                        }
                    }

                    // Auto-focus no botão principal após 2 segundos
                    setTimeout(function() {
                        const primaryBtn = document.querySelector('.btn-primary');
                        if (primaryBtn) {
                            primaryBtn.focus();
                        }
                    }, 2000);
                </script>
            </body>
            </html>
            """.formatted(
                titulo, titulo, mensagem,
                detalhes.isEmpty() ? "" : "<div class=\"error-details\">" + detalhes + "</div>",
                loginUrl, logoutGovBrUrl, logoutUrl
        );
    }

    private static String obterBaseUrl(AuthenticationFlowContext context) {
        String baseUrl = context.getSession().getContext().getUri().getBaseUri().toString();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String construirUrlLogin(AuthenticationFlowContext context, String baseUrl) {
        try {
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
                    .append("&prompt=login"); // Força logout automático e novo login

            if (redirectUri != null && !redirectUri.trim().isEmpty()) {
                loginUrl.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
            }

            if (state != null && !state.trim().isEmpty()) {
                loginUrl.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
            }

            // Adiciona nonce se disponível
            String nonce = authSession.getClientNote("nonce");
            if (nonce != null && !nonce.trim().isEmpty()) {
                loginUrl.append("&nonce=").append(URLEncoder.encode(nonce, StandardCharsets.UTF_8));
            }

            // Adiciona response_mode se disponível
            String responseMode = authSession.getClientNote("response_mode");
            if (responseMode != null && !responseMode.trim().isEmpty()) {
                loginUrl.append("&response_mode=").append(URLEncoder.encode(responseMode, StandardCharsets.UTF_8));
            }

            return loginUrl.toString();

        } catch (Exception e) {
            return baseUrl + "/realms/" + context.getRealm().getName() + "/protocol/openid-connect/auth?prompt=login";
        }
    }

    private static String construirUrlLogout(String baseUrl, String realmName) {
        return baseUrl + "/realms/" + realmName + "/protocol/openid-connect/logout";
    }

    private static String construirUrlLogoutGovBr(AuthenticationFlowContext context) {
        try {
            var authSession = context.getAuthenticationSession();
            String postLogoutRedirectUri = authSession.getRedirectUri();

            StringBuilder logoutUrl = new StringBuilder();
            logoutUrl.append(LOGOUT_URL);

            if (postLogoutRedirectUri != null && !postLogoutRedirectUri.trim().isEmpty()) {
                logoutUrl.append("?post_logout_redirect_uri=")
                        .append(URLEncoder.encode(postLogoutRedirectUri, StandardCharsets.UTF_8));
            }

            return logoutUrl.toString();

        } catch (Exception e) {
            return LOGOUT_URL;
        }
    }

    /**
     * Métodos de conveniência para tipos específicos de erro
     */
    public static Response erroNivelInsuficiente(AuthenticationFlowContext context,
                                                 String nivelAtual) {
        return criarPaginaErro(
                context,
                "Nível de Autenticação Insuficiente",
                String.format("Seu nível atual (%s) não permite acesso a este sistema.", nivelAtual),
                "É necessário possuir nível <strong>Ouro</strong> no Gov.br para acessar este sistema. " +
                        "Você pode aumentar seu nível validando seus dados bancários (Prata) ou " +
                        "comparecendo presencialmente a um posto de atendimento (Ouro)."
        );
    }

    public static Response erroTokenInvalido(AuthenticationFlowContext context) {
        return criarPaginaErro(
                context,
                "Token de Autenticação Inválido",
                "Não foi possível validar sua autenticação Gov.br.",
                "Seu token de acesso pode ter expirado ou estar inválido. " +
                        "Tente fazer login novamente."
        );
    }

    public static Response erroServicoIndisponivel(AuthenticationFlowContext context) {
        return criarPaginaErro(
                context,
                "Serviço Temporariamente Indisponível",
                "Não foi possível verificar seu nível de autenticação no momento.",
                "O serviço Gov.br está temporariamente indisponível. " +
                        "Tente novamente em alguns minutos."
        );
    }
}