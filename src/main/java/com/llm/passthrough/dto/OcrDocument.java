package com.llm.passthrough.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OcrDocument {

    @NotBlank(message = "Document type is required")
    private String type;

    @JsonProperty("document_url")
    @NotBlank(message = "Document URL is required")
    private String documentUrl;
}
