package br.com.spi.govbr.service;

import br.com.spi.govbr.config.GovBrConfig;
import br.com.spi.govbr.dto.ValidationResult;
import org.jboss.logging.Logger;

import java.util.Set;

public class LevelValidationService {

    private static final Logger logger = Logger.getLogger(LevelValidationService.class);

    private final GovBrApiClient apiClient;
    private final Set<String> niveisAceitos;

    public LevelValidationService() {
        this.apiClient = new GovBrApiClient();
        this.niveisAceitos = Set.of(GovBrConfig.ACCEPTED_LEVELS);
    }

    public ValidationResult validarNivelUsuario(String accessToken) {

        if (accessToken == null || accessToken.trim().isEmpty()) {
            return ValidationResult.error("Token Gov.br não encontrado");
        }

        try {
            String nivelUsuario = apiClient.consultarNivelUsuario(accessToken);

            logger.infof("Nível do usuário: %s", nivelUsuario);

            if (niveisAceitos.contains(nivelUsuario)) {
                logger.infof("✅ Login aprovado - Nível: %s", nivelUsuario);
                return ValidationResult.success(nivelUsuario);
            } else {
                String mensagem = String.format("Nível %s insuficiente. É necessário nível Ouro.", nivelUsuario);
                logger.warnf("❌ Login rejeitado - %s", mensagem);
                return ValidationResult.failure(nivelUsuario, mensagem);
            }

        } catch (Exception e) {
            logger.errorf("Erro na validação: %s", e.getMessage());

            String mensagem = e.getMessage().contains("Token") ?
                    "Token Gov.br inválido ou expirado" :
                    "Serviço Gov.br temporariamente indisponível";

            return ValidationResult.error(mensagem);
        }
    }
}