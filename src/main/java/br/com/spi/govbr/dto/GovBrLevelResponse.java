package br.com.spi.govbr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para resposta da API de nível Gov.br
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GovBrLevelResponse(
        @JsonProperty("id") String id,
        @JsonProperty("dataAtualizacao") String dataAtualizacao
) {
    /**
     * Valida se a resposta contém dados válidos
     * CORRIGIDO: Apenas o ID é obrigatório
     */
    public boolean isValid() {
        return id != null && !id.trim().isEmpty();
    }
}