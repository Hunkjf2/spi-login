package br.com.spi.govbr.service;

import br.com.spi.govbr.config.GovBrValidatorConfig;
import br.com.spi.govbr.constants.GovBrLevelConstants;
import br.com.spi.govbr.dto.ValidationResult;
import br.com.spi.govbr.exception.GovBrValidationException;
import org.jboss.logging.Logger;
import java.util.Arrays;
import java.util.Set;

/**
 * Serviço responsável pela validação de nível de autenticação Gov.br
 */
public class LevelValidationService {

    private static final Logger logger = Logger.getLogger(LevelValidationService.class);

    private final GovBrApiClient apiClient;
    private final Set<String> niveisAceitos;

    public LevelValidationService() {
        this.apiClient = new GovBrApiClient();
        this.niveisAceitos = Set.of(GovBrValidatorConfig.getAcceptedLevels());

        logger.infof("LevelValidationService inicializado. Níveis aceitos: %s",
                Arrays.toString(GovBrValidatorConfig.getAcceptedLevels()));
    }

    /**
     * Valida se o nível do usuário atende aos critérios mínimos configurados
     *
     * @param accessToken Token de acesso Gov.br
     * @return Resultado da validação
     */
    public ValidationResult validarNivelUsuario(String accessToken) {

        logger.infof("Iniciando validação de nível Gov.br");

        // Validação básica do token
        if (accessToken == null || accessToken.trim().isEmpty()) {
            logger.warn("Token de acesso Gov.br não fornecido");
            return ValidationResult.error(
                    GovBrValidatorConfig.getErrorInvalidToken(),
                    "MISSING_TOKEN"
            );
        }

        try {
            // Consulta o nível na API Gov.br
            String nivelUsuario = apiClient.consultarNivelUsuario(accessToken);

            logger.infof("Nível do usuário obtido da API: %s", nivelUsuario);

            // Valida se o nível atende aos critérios
            boolean nivelAceito = isNivelAceito(nivelUsuario);

            if (nivelAceito) {
                logger.infof("Validação aprovada - Nível %s é aceito para login", nivelUsuario);
                return ValidationResult.success(nivelUsuario);
            } else {
                String mensagem = construirMensagemNivelInsuficiente(nivelUsuario);
                logger.warnf("Validação rejeitada - %s", mensagem);
                return ValidationResult.failure(nivelUsuario, mensagem, "INSUFFICIENT_LEVEL");
            }

        } catch (GovBrValidationException e) {
            logger.errorf("Erro de validação Gov.br: %s [Código: %s]", e.getMessage(), e.getErrorCode());

            // Mapeia erros específicos para mensagens amigáveis
            String mensagemUsuario = mapearErroParaMensagem(e);
            return ValidationResult.error(mensagemUsuario, e.getErrorCode());

        } catch (Exception e) {
            logger.errorf("Erro inesperado na validação: %s", e.getMessage());
            return ValidationResult.error(
                    GovBrValidatorConfig.getErrorApiUnavailable(),
                    "UNEXPECTED_ERROR"
            );
        }
    }

    /**
     * Verifica se o nível está na lista de níveis aceitos
     */
    private boolean isNivelAceito(String nivel) {
        if (nivel == null) {
            return false;
        }

        boolean aceito = niveisAceitos.contains(nivel);

        logger.debugf("Verificação de nível: %s -> %s", nivel, aceito ? "ACEITO" : "REJEITADO");
        return aceito;
    }

    /**
     * Constrói mensagem personalizada para nível insuficiente baseada no nível atual
     */
    private String construirMensagemNivelInsuficiente(String nivelAtual) {
        // Se for especificamente Bronze, usa mensagem específica
        if (GovBrLevelConstants.BRONZE.equals(nivelAtual)) {
            return GovBrValidatorConfig.getErrorBronzeLevel();
        }

        // Para outros casos (nível desconhecido, etc)
        return String.format(
                "Seu nível de autenticação Gov.br (%s) não é suficiente para acessar este sistema. " +
                        "É necessário nível Prata ou Ouro. Acesse gov.br para verificar e elevar seu nível de confiabilidade.",
                nivelAtual != null ? nivelAtual : "Desconhecido"
        );
    }

    /**
     * Mapeia exceções específicas para mensagens amigáveis ao usuário
     */
    private String mapearErroParaMensagem(GovBrValidationException e) {
        return switch (e.getErrorCode()) {
            case "INVALID_TOKEN" -> GovBrValidatorConfig.getErrorInvalidToken();
            case "API_UNAVAILABLE", "API_ERROR", "SERVICE_NOT_FOUND", "PARSE_ERROR", "EMPTY_RESPONSE", "NO_LEVEL_FOUND", "INVALID_LEVEL_DATA" ->
                    GovBrValidatorConfig.getErrorApiUnavailable();
            default -> "Erro ao validar seu nível Gov.br. Tente fazer login novamente.";
        };
    }

    /**
     * Valida configuração do serviço
     */
    public boolean isConfigurationValid() {
        try {
            String[] acceptedLevels = GovBrValidatorConfig.getAcceptedLevels();
            String apiUrl = GovBrValidatorConfig.getNivelApiUrl();

            boolean valid = acceptedLevels != null && acceptedLevels.length > 0 &&
                    apiUrl != null && !apiUrl.trim().isEmpty();

            if (!valid) {
                logger.error("Configuração inválida do LevelValidationService");
            }

            return valid;
        } catch (Exception e) {
            logger.errorf("Erro ao validar configuração: %s", e.getMessage());
            return false;
        }
    }
}