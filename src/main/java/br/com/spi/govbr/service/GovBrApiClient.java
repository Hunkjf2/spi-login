package br.com.spi.govbr.service;

import br.com.spi.govbr.config.GovBrValidatorConfig;
import br.com.spi.govbr.constants.GovBrLevelConstants;
import br.com.spi.govbr.dto.GovBrLevelResponse;
import br.com.spi.govbr.exception.GovBrValidationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class GovBrApiClient {

    private static final Logger logger = Logger.getLogger(GovBrApiClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GovBrApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(GovBrValidatorConfig.getConnectTimeout()))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Consulta o nível de autenticação do usuário na API Gov.br
     */
    public String consultarNivelUsuario(String accessToken) throws GovBrValidationException {
        try {
            logger.infof("Consultando nível do usuário na API Gov.br");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GovBrValidatorConfig.getNivelApiUrl()))
                    .header(GovBrLevelConstants.AUTHORIZATION_HEADER,
                            GovBrLevelConstants.BEARER_PREFIX + accessToken)
                    .header(GovBrLevelConstants.ACCEPT_HEADER,
                            GovBrLevelConstants.CONTENT_TYPE_JSON)
                    .timeout(Duration.ofSeconds(GovBrValidatorConfig.getRequestTimeout()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new GovBrValidationException(
                        "API Gov.br retornou status: " + response.statusCode());
            }

            return processarRespostaApi(response.body());

        } catch (Exception e) {
            logger.errorf("Erro ao consultar API Gov.br: %s", e.getMessage());
            throw new GovBrValidationException("Erro ao consultar nível na API Gov.br", e);
        }
    }

    private String processarRespostaApi(String responseBody) throws GovBrValidationException {
        try {
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new GovBrValidationException("Resposta da API está vazia");
            }

            List<GovBrLevelResponse> responses = objectMapper.readValue(
                    responseBody, new TypeReference<List<GovBrLevelResponse>>() {});

            if (responses.isEmpty()) {
                throw new GovBrValidationException("Nenhum nível encontrado na resposta");
            }

            GovBrLevelResponse levelResponse = responses.get(0);
            if (!levelResponse.isValid()) {
                throw new GovBrValidationException("Resposta da API contém dados inválidos");
            }

            return converterCodigoParaNivel(levelResponse.id());

        } catch (Exception e) {
            logger.errorf("Erro ao processar resposta da API: %s", e.getMessage());
            throw new GovBrValidationException("Erro ao processar resposta da API Gov.br", e);
        }
    }

    private String converterCodigoParaNivel(String codigo) {
        return switch (codigo) {
            case GovBrLevelConstants.NIVEL_BRONZE_CODE -> GovBrLevelConstants.BRONZE;
            case GovBrLevelConstants.NIVEL_PRATA_CODE -> GovBrLevelConstants.PRATA;
            case GovBrLevelConstants.NIVEL_OURO_CODE -> GovBrLevelConstants.OURO;
            default -> {
                logger.warnf("Código de nível desconhecido: %s", codigo);
                yield GovBrLevelConstants.BRONZE; // Default para Bronze
            }
        };
    }
}