package com.llm.passthrough.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "apigee")
public class ApigeeProperties {

    @NotBlank(message = "APIGEE URL is required")
    private String url;

    @NotBlank(message = "OCR URL is required")
    private String ocrUrl;

    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "Client Secret is required")
    private String clientSecret;

    private Ssl ssl = new Ssl();

    @Data
    public static class Ssl {
        private boolean enabled = true;
        private String keyStorePath;
        private String keyStorePassword;
        private String trustStorePath;
        private String trustStorePassword;
        private String tlsKeyPath;
        private String tlsCertPath;
        private String caCertPath;
    }
}
