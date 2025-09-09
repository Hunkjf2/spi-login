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

/**
 * Cliente HTTP para integração com APIs Gov.br
 */
public class GovBrApiClient {

    private static final Logger logger = Logger.getLogger(GovBrApiClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GovBrApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(GovBrValidatorConfig.getConnectTimeout()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Consulta o nível de autenticação do usuário na API Gov.br
     *
     * @param accessToken Token de acesso Gov.br
     * @return Nível do usuário (Bronze, Prata, Ouro)
     * @throws GovBrValidationException Em caso de erro na consulta
     */
    public String consultarNivelUsuario(String accessToken) throws GovBrValidationException {

        logger.infof("Iniciando consulta de nível na API Gov.br");

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw GovBrValidationException.invalidToken();
        }

        try {
            HttpRequest request = construirRequisicao(accessToken);
            HttpResponse<String> response = executarRequisicao(request);

            validarStatusResponse(response);

            String nivel = processarRespostaApi(response.body());

            logger.infof("Consulta de nível concluída com sucesso: %s", nivel);
            return nivel;

        } catch (GovBrValidationException e) {
            throw e; // Re-lança exceções específicas
        } catch (Exception e) {
            logger.errorf("Erro inesperado ao consultar API Gov.br: %s", e.getMessage());
            throw GovBrValidationException.apiUnavailable(e);
        }
    }

    /**
     * Constrói a requisição HTTP para a API Gov.br
     */
    private HttpRequest construirRequisicao(String accessToken) {
        return HttpRequest.newBuilder()
                .uri(URI.create(GovBrValidatorConfig.getNivelApiUrl()))
                .header(GovBrLevelConstants.AUTHORIZATION_HEADER,
                        GovBrLevelConstants.BEARER_PREFIX + accessToken)
                .header(GovBrLevelConstants.ACCEPT_HEADER,
                        GovBrLevelConstants.CONTENT_TYPE_JSON)
                .header(GovBrLevelConstants.USER_AGENT_HEADER,
                        GovBrLevelConstants.USER_AGENT_VALUE)
                .timeout(Duration.ofSeconds(GovBrValidatorConfig.getRequestTimeout()))
                .GET()
                .build();
    }

    /**
     * Executa a requisição HTTP
     */
    private HttpResponse<String> executarRequisicao(HttpRequest request) throws Exception {
        logger.debug("Executando requisição para API Gov.br");

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        logger.infof("Resposta recebida - Status: %d, Body: %s",
                response.statusCode(),
                response.body() != null ? response.body() : "null");

        return response;
    }

    /**
     * Valida o status code da resposta HTTP
     */
    private void validarStatusResponse(HttpResponse<String> response) throws GovBrValidationException {
        int statusCode = response.statusCode();

        switch (statusCode) {
            case GovBrLevelConstants.HTTP_OK:
                return; // Sucesso

            case GovBrLevelConstants.HTTP_UNAUTHORIZED:
            case GovBrLevelConstants.HTTP_FORBIDDEN:
                logger.warnf("Token Gov.br inválido ou expirado - Status: %d", statusCode);
                throw GovBrValidationException.invalidToken();

            case GovBrLevelConstants.HTTP_NOT_FOUND:
                logger.errorf("Endpoint da API Gov.br não encontrado - Status: %d", statusCode);
                throw new GovBrValidationException("Serviço Gov.br não encontrado", "SERVICE_NOT_FOUND");

            case GovBrLevelConstants.HTTP_INTERNAL_ERROR:
            default:
                logger.errorf("API Gov.br retornou erro - Status: %d", statusCode);
                throw new GovBrValidationException("Erro interno do serviço Gov.br", "API_ERROR");
        }
    }

    /**
     * Processa a resposta JSON da API Gov.br
     */
    private String processarRespostaApi(String responseBody) throws GovBrValidationException {
        try {
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new GovBrValidationException("Resposta da API Gov.br está vazia", "EMPTY_RESPONSE");
            }

            logger.infof("Processando resposta da API: %s", responseBody);

            List<GovBrLevelResponse> responses = objectMapper.readValue(
                    responseBody, new TypeReference<>() {
                    });

            if (responses == null || responses.isEmpty()) {
                throw new GovBrValidationException("Nenhum nível encontrado na resposta da API", "NO_LEVEL_FOUND");
            }

            GovBrLevelResponse levelResponse = responses.get(0);

            logger.infof("Dados do nível recebidos: id=%s, dataAtualizacao=%s",
                    levelResponse.id(), levelResponse.dataAtualizacao());

            if (!levelResponse.isValid()) {
                logger.warnf("Resposta da API contém ID inválido: id=%s", levelResponse.id());
                throw new GovBrValidationException("ID de nível inválido retornado pela API", "INVALID_LEVEL_DATA");
            }

            return converterCodigoParaNivel(levelResponse.id());

        } catch (GovBrValidationException e) {
            throw e; // Re-lança exceções específicas
        } catch (Exception e) {
            logger.errorf("Erro ao fazer parsing da resposta da API: %s", e.getMessage());
            throw new GovBrValidationException("Erro ao processar resposta da API Gov.br", "PARSE_ERROR", e);
        }
    }

    /**
     * Converte código numérico retornado pela API para nome do nível
     */
    private String converterCodigoParaNivel(String codigo) {
        if (codigo == null) {
            logger.warn("Código de nível é null, assumindo Bronze");
            return GovBrLevelConstants.BRONZE;
        }

        String nivel = switch (codigo.trim()) {
            case GovBrLevelConstants.NIVEL_BRONZE_CODE -> GovBrLevelConstants.BRONZE;
            case GovBrLevelConstants.NIVEL_PRATA_CODE -> GovBrLevelConstants.PRATA;
            case GovBrLevelConstants.NIVEL_OURO_CODE -> GovBrLevelConstants.OURO;
            default -> {
                logger.warnf("Código de nível desconhecido: '%s', assumindo Bronze", codigo);
                yield GovBrLevelConstants.BRONZE;
            }
        };

        logger.infof("Código '%s' convertido para nível '%s'", codigo, nivel);
        return nivel;
    }
}