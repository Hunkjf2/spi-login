package br.com.spi.govbr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GovBrLevelResponse(String id, String descricao) {}