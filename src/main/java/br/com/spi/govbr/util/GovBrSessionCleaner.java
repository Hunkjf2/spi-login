package br.com.spi.govbr.util;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;

/**
 * Utilitário para limpeza de sessões Gov.br
 */
public class GovBrSessionCleaner {

    private static final Logger logger = Logger.getLogger(GovBrSessionCleaner.class);

    private GovBrSessionCleaner() {}

    /**
     * Limpa todas as sessões do usuário e dados de autenticação
     */
    public static void limparSessoesUsuario(AuthenticationFlowContext context) {
        try {
            UserModel user = context.getUser();
            if (user != null) {
                // Remove todas as sessões ativas do usuário
                context.getSession().sessions()
                        .getUserSessionsStream(context.getRealm(), user)
                        .forEach(session -> {
                            logger.infof("Removendo sessão: %s para usuário: %s",
                                    session.getId(), user.getUsername());
                            context.getSession().sessions()
                                    .removeUserSession(context.getRealm(), session);
                        });

                logger.infof("Sessões do usuário %s removidas com sucesso", user.getUsername());
            }

            // Limpa dados da sessão de autenticação atual
            var authSession = context.getAuthenticationSession();
            if (authSession != null) {
                // Remove notas específicas do Gov.br
                authSession.removeAuthNote("FEDERATED_ACCESS_TOKEN");
                authSession.removeAuthNote("FEDERATED_IDENTITY_ID");
                authSession.removeAuthNote("FEDERATED_USERNAME");

                // Limpa outras notas da sessão
                authSession.getUserSessionNotes().clear();
                authSession.getClientNotes().clear();

                logger.info("Dados de autenticação Gov.br removidos da sessão");
            }

        } catch (Exception e) {
            logger.errorf("Erro ao limpar sessões: %s", e.getMessage());
        }
    }

    /**
     * Verifica se o usuário possui sessões ativas
     */
    public static boolean usuarioPossuiSessoesAtivas(AuthenticationFlowContext context) {
        try {
            UserModel user = context.getUser();
            if (user == null) {
                return false;
            }

            long sessionsCount = context.getSession().sessions()
                    .getUserSessionsStream(context.getRealm(), user)
                    .count();

            logger.infof("Usuário %s possui %d sessões ativas", user.getUsername(), sessionsCount);
            return sessionsCount > 0;

        } catch (Exception e) {
            logger.errorf("Erro ao verificar sessões ativas: %s", e.getMessage());
            return false;
        }
    }
}