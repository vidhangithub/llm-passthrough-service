package com.llm.passthrough.controller;

import com.llm.passthrough.dto.OcrRequest;
import com.llm.passthrough.dto.OcrResponse;
import com.llm.passthrough.service.OcrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/mistral")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @PostMapping(value = "/ocr", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OcrResponse> processOcr(@Valid @RequestBody OcrRequest request) {
        log.info("Received OCR request - Model: {}, Document Type: {}",
                request.getModel(), request.getDocument().getType());

        OcrResponse response = ocrService.processOcr(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ocr/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
