package com.llm.passthrough.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.passthrough.dto.ChatRequest;
import com.llm.passthrough.dto.ChatResponse;
import com.llm.passthrough.exception.ApigeeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

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

    public StreamingResponseBody chatStream(ChatRequest request) {
        log.info("Sending streaming chat request to APIGEE - Model: {}", request.getModel());

        // Ensure stream is enabled
        request.setStream(true);

        return outputStream -> {
            try {
                restClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .body(request)
                        .exchange((req, res) -> {
                            if (res.getStatusCode().isError()) {
                                String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                log.error("APIGEE streaming error: {} - {}", res.getStatusCode(), body);
                                outputStream.write(("data: {\"error\": \"" + body + "\"}\n\n")
                                        .getBytes(StandardCharsets.UTF_8));
                                return null;
                            }

                            try (InputStream is = res.getBody();
                                 BufferedReader reader = new BufferedReader(
                                         new InputStreamReader(is, StandardCharsets.UTF_8))) {

                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (!line.isEmpty()) {
                                        outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                                        outputStream.flush();
                                    }
                                }
                            }
                            return null;
                        });
            } catch (Exception e) {
                log.error("Error during streaming: ", e);
                outputStream.write(("data: {\"error\": \"" + e.getMessage() + "\"}\n\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
        };
    }
}
