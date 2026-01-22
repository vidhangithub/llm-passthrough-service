package com.llm.passthrough.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Choice {

    @JsonProperty("finish_reason")
    private String finishReason;

    private Integer index;
    private ResponseMessage message;

    // For streaming responses
    private ResponseMessage delta;
}
