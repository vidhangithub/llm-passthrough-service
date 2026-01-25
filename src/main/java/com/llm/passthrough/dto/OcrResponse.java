package com.llm.passthrough.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OcrResponse {

    private List<OcrPage> pages;
    private String model;

    @JsonProperty("document_annotation")
    private Object documentAnnotation;

    @JsonProperty("usage_info")
    private OcrUsageInfo usageInfo;
}
