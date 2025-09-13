package br.com.spi.govbr.authenticator;

import br.com.spi.govbr.config.GovBrConfig;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import java.util.List;

public class GovBrLevelAuthenticatorFactory implements AuthenticatorFactory {

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getDisplayType() {
        return GovBrConfig.AUTHENTICATOR_NAME;
    }

    @Override
    public String getReferenceCategory() {
        return "validation";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Valida se o usuário Gov.br possui nível necessário antes de permitir o login.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new GovBrLevelAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
        // Não implementado
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Não implementado
    }

    @Override
    public void close() {
        // Não implementado
    }

    @Override
    public String getId() {
        return GovBrConfig.AUTHENTICATOR_ID;
    }
}