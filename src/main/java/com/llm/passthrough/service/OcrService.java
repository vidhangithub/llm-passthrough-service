package com.llm.passthrough.service;

import com.llm.passthrough.dto.OcrRequest;
import com.llm.passthrough.dto.OcrResponse;
import com.llm.passthrough.exception.ApigeeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class OcrService {

    private final RestClient ocrRestClient;

    public OcrService(@Qualifier("ocrRestClient") RestClient ocrRestClient) {
        this.ocrRestClient = ocrRestClient;
    }

    public OcrResponse processOcr(OcrRequest request) {
        log.info("Sending OCR request to APIGEE - Model: {}, Document Type: {}",
                request.getModel(), request.getDocument().getType());

        try {
            OcrResponse response = ocrRestClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.error("APIGEE OCR error response: {} - {}", res.getStatusCode(), body);
                        throw new ApigeeException("APIGEE OCR request failed: " + body,
                                res.getStatusCode().value(), body);
                    })
                    .body(OcrResponse.class);

            log.info("Received OCR response from APIGEE - Model: {}, Pages: {}",
                    response != null ? response.getModel() : "null",
                    response != null && response.getPages() != null ? response.getPages().size() : 0);
            return response;

        } catch (ApigeeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling APIGEE OCR: ", e);
            throw new ApigeeException("Failed to communicate with APIGEE OCR", e);
        }
    }
}
