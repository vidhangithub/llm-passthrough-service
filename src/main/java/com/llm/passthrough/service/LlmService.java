package com.llm.passthrough.service;

import com.llm.passthrough.dto.ChatRequest;
import com.llm.passthrough.dto.ChatResponse;
import com.llm.passthrough.exception.ApigeeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final RestClient restClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatResponse chat(ChatRequest request) {
        log.info("Sending chat request to APIGEE - Model: {}, Messages: {}",
                request.getModel(), request.getMessages().size());

        try {
            ChatResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.error("APIGEE error response: {} - {}", res.getStatusCode(), body);
                        throw new ApigeeException("APIGEE request failed: " + body,
                                res.getStatusCode().value(), body);
                    })
                    .body(ChatResponse.class);

            log.info("Received response from APIGEE - ID: {}",
                    response != null ? response.getId() : "null");
            return response;

        } catch (ApigeeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling APIGEE: ", e);
            throw new ApigeeException("Failed to communicate with APIGEE", e);
        }
    }

    public SseEmitter chatStream(ChatRequest request) {
        log.info("Sending streaming chat request to APIGEE - Model: {}", request.getModel());

        // Ensure stream is enabled
        request.setStream(true);

        // Create SSE emitter with 5 minute timeout
        SseEmitter emitter = new SseEmitter(300000L);

        executor.execute(() -> {
            try {
                restClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .body(request)
                        .exchange((req, res) -> {
                            if (res.getStatusCode().isError()) {
                                String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                log.error("APIGEE streaming error: {} - {}", res.getStatusCode(), body);
                                emitter.send(SseEmitter.event().data("{\"error\": \"" + body + "\"}"));
                                emitter.complete();
                                return null;
                            }

                            try (InputStream is = res.getBody();
                                 BufferedReader reader = new BufferedReader(
                                         new InputStreamReader(is, StandardCharsets.UTF_8))) {

                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (!line.isEmpty()) {
                                        // Send raw line as SSE data
                                        emitter.send(SseEmitter.event().data(line));
                                    }
                                }
                                emitter.complete();
                            }
                            return null;
                        });
            } catch (Exception e) {
                log.error("Error during streaming: ", e);
                try {
                    emitter.send(SseEmitter.event().data("{\"error\": \"" + e.getMessage() + "\"}"));
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("Error sending error event: ", ex);
                }
            }
        });

        emitter.onCompletion(() -> log.info("SSE connection completed"));
        emitter.onTimeout(() -> log.warn("SSE connection timed out"));
        emitter.onError(e -> log.error("SSE error: ", e));

        return emitter;
    }
}
