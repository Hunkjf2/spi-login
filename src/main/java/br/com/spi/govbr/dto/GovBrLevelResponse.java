package br.com.spi.govbr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GovBrLevelResponse(
        @JsonProperty("id") String id,
        @JsonProperty("dataAtualizacao") String dataAtualizacao
) {
    public boolean isValid() {
        return id != null && !id.trim().isEmpty();
    }
}