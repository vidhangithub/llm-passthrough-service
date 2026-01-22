package com.llm.passthrough.controller;

import com.llm.passthrough.dto.ChatRequest;
import com.llm.passthrough.dto.ChatResponse;
import com.llm.passthrough.service.LlmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final LlmService llmService;

    /**
     * Non-streaming chat completions endpoint.
     * Use this when stream=false or stream is not specified in the request body.
     */
    @PostMapping(value = "/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> chatCompletions(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat completion request - Stream: {}", request.getStream());

        // Force non-streaming for this endpoint
        request.setStream(false);

        ChatResponse response = llmService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Streaming chat completions endpoint.
     * Use this endpoint for SSE streaming responses.
     */
    @PostMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatCompletionsStream(@Valid @RequestBody ChatRequest request) {
        log.info("Received streaming chat completion request");
        return llmService.chatStream(request);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
