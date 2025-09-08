package br.com.spi.govbr.service;

import br.com.spi.govbr.config.GovBrValidatorConfig;
import br.com.spi.govbr.constants.GovBrLevelConstants;
import br.com.spi.govbr.dto.ValidationResult;
import br.com.spi.govbr.exception.GovBrValidationException;
import br.com.spi.govbr.service.GovBrApiClient;
import org.jboss.logging.Logger;

import java.util.Arrays;

public class LevelValidationService {

    private static final Logger logger = Logger.getLogger(LevelValidationService.class);
    private final GovBrApiClient apiClient;

    public LevelValidationService() {
        this.apiClient = new GovBrApiClient();
    }

    /**
     * Valida se o nível do usuário atende aos critérios mínimos
     */
    public ValidationResult validarNivelUsuario(String accessToken) {
        try {
            logger.info("Iniciando validação de nível Gov.br");

            if (accessToken == null || accessToken.trim().isEmpty()) {
                return ValidationResult.error("Token de acesso não encontrado");
            }

            String nivelUsuario = apiClient.consultarNivelUsuario(accessToken);
            logger.infof("Nível do usuário obtido: %s", nivelUsuario);

            boolean nivelAceito = isNivelAceito(nivelUsuario);

            if (nivelAceito) {
                logger.infof("Nível %s é aceito para login", nivelUsuario);
                return ValidationResult.success(nivelUsuario);
            } else {
                String mensagem = String.format(
                        "Nível %s insuficiente. Requer nível Prata ou Ouro.", nivelUsuario);
                logger.warnf("Login bloqueado: %s", mensagem);
                return ValidationResult.failure(nivelUsuario, mensagem);
            }

        } catch (GovBrValidationException e) {
            logger.errorf("Erro na validação Gov.br: %s", e.getMessage());
            return ValidationResult.error("Erro ao validar nível de autenticação. Tente novamente.");
        }
    }

    /**
     * Verifica se o nível está na lista de níveis aceitos
     */
    private boolean isNivelAceito(String nivel) {
        String[] niveisAceitos = GovBrValidatorConfig.getAcceptedLevels();
        return Arrays.asList(niveisAceitos).contains(nivel);
    }
}