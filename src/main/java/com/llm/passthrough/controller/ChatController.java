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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final LlmService llmService;

    @PostMapping("/completions")
    public ResponseEntity<?> chatCompletions(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat completion request - Stream: {}", request.getStream());

        if (Boolean.TRUE.equals(request.getStream())) {
            StreamingResponseBody stream = llmService.chatStream(request);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(stream);
        }

        ChatResponse response = llmService.chat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
