package br.com.spi.govbr.service;

import br.com.spi.govbr.config.GovBrConfig;
import br.com.spi.govbr.dto.GovBrLevelResponse;
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
                .connectTimeout(Duration.ofSeconds(GovBrConfig.CONNECT_TIMEOUT))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String consultarNivelUsuario(String accessToken) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GovBrConfig.NIVEL_API_URL))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(GovBrConfig.REQUEST_TIMEOUT))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Token inválido");
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("API Gov.br indisponível");
        }

        List<GovBrLevelResponse> responses = objectMapper.readValue(
                response.body(), new TypeReference<>() {});

        if (responses == null || responses.isEmpty()) {
            throw new RuntimeException("Nenhum nível encontrado");
        }

        return converterCodigoParaNivel(responses.get(0).id());
    }

    private String converterCodigoParaNivel(String codigo) {
        return switch (codigo) {
            case "1" -> "Bronze";
            case "2" -> "Prata";
            case "3" -> "Ouro";
            default -> "Bronze";
        };
    }
}