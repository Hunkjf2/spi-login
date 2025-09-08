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

public class GovBrLevelAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String REQUIREMENT_CHOICES[] = {
            AuthenticationExecutionModel.Requirement.REQUIRED.name(),
            AuthenticationExecutionModel.Requirement.ALTERNATIVE.name(),
            AuthenticationExecutionModel.Requirement.DISABLED.name()
    };

    @Override
    public String getDisplayType() {
        return GovBrValidatorConfig.getAuthenticatorDisplayType();
    }

    @Override
    public String getReferenceCategory() {
        return "validation";
    }

    @Override
    public boolean isConfigurable() {
        return false; // Sem configurações adicionais por enquanto
    }

    @Override
    public String[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Valida se o usuário Gov.br possui nível Prata ou Ouro antes de permitir o login";
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
        // Inicialização se necessária
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Pós-inicialização se necessária
    }

    @Override
    public void close() {
        // Cleanup se necessário
    }

    @Override
    public String getId() {
        return GovBrValidatorConfig.getAuthenticatorProviderId();
    }
}