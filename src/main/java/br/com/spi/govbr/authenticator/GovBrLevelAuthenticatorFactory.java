package br.com.spi.govbr.authenticator;

import br.com.spi.govbr.config.GovBrValidatorConfig;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import java.util.Arrays;
import java.util.List;

/**
 * Factory responsável por criar instâncias do GovBrLevelAuthenticator
 */
public class GovBrLevelAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    // Opções de requisito disponíveis para este authenticator
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getDisplayType() {
        return GovBrValidatorConfig.getAuthenticatorDisplayType();
    }

    @Override
    public String getReferenceCategory() {
        return "validation"; // Categoria de validação
    }

    @Override
    public boolean isConfigurable() {
        return false; // Por enquanto não há configurações adicionais via UI
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false; // Usuário não pode configurar este authenticator
    }

    @Override
    public String getHelpText() {
        return "Valida se o usuário Gov.br possui nível Prata ou Ouro antes de permitir o login. " +
                "Este authenticator deve ser configurado APÓS o Identity Provider Redirector no fluxo de autenticação.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new GovBrLevelAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
        // Inicialização da factory se necessária
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Pós-inicialização se necessária
    }

    @Override
    public void close() {
        // Cleanup de recursos da factory se necessário
    }

    @Override
    public String getId() {
        return GovBrValidatorConfig.getAuthenticatorProviderId();
    }
}