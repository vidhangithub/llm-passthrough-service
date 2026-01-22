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
public class ChatResponse {

    private String id;
    private Long created;
    private String model;
    private String object;

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    private List<Choice> choices;
    private Usage usage;
}
