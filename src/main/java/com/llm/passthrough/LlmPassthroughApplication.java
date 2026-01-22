package com.llm.passthrough;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class LlmPassthroughApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmPassthroughApplication.class, args);
    }
}
