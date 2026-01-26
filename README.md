# LLM Passthrough Service

A Spring Boot service that acts as a proxy for LLM APIs, providing a unified interface for chat completions and OCR capabilities with SSL/TLS support.

## Features

- Chat completions API (streaming and non-streaming)
- Multimodal support (text + images)
- Mistral OCR API
- SSL/TLS with PEM certificate support
- Configurable authentication headers

## Requirements

- Java 17+
- Maven 3.6+

## Configuration

Configure the service via `application.yml` or environment variables:

```yaml
apigee:
  url: https://your-api-endpoint/v1/chat/completions
  ocr-url: https://your-api-endpoint/v1/mistral/ocr
  client-id: ${APIGEE_CLIENT_ID}
  client-secret: ${APIGEE_CLIENT_SECRET}
  ssl:
    enabled: true
    tls-key-path: classpath:certs/tls.key
    tls-cert-path: classpath:certs/tls.crt
    ca-cert-path: classpath:certs/ca.crt
```

### Environment Variables

| Variable | Description |
|----------|-------------|
| `APIGEE_CLIENT_ID` | Client ID for API authentication |
| `APIGEE_CLIENT_SECRET` | Client secret for API authentication |

## API Endpoints

### Chat Completions

#### Non-Streaming

```
POST /api/v1/chat/completions
Content-Type: application/json
```

**Simple Text Request:**
```json
{
  "model": "gpt-4",
  "messages": [
    {
      "role": "user",
      "content": "Hello, how are you?"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 1000
}
```

**Multimodal Request (Text + Image):**
```json
{
  "model": "vertex_ai/gemini-2.0-flash-001",
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "Describe the image"
        },
        {
          "type": "inline_data",
          "inline_data": {
            "data": "data:image/png;base64,{base64_encoded_image}"
          }
        }
      ]
    }
  ]
}
```

**Response:**
```json
{
  "id": "chatcmpl-123",
  "created": 1734366691,
  "model": "vertex_ai/gemini-2.0-flash-001",
  "object": "chat.completion",
  "system_fingerprint": null,
  "choices": [
    {
      "finish_reason": "stop",
      "index": 0,
      "message": {
        "content": "The image shows...",
        "role": "assistant",
        "tool_calls": null,
        "function_call": null
      }
    }
  ],
  "usage": {
    "completion_tokens": 43,
    "prompt_tokens": 13,
    "total_tokens": 56,
    "completion_tokens_details": null,
    "prompt_tokens_details": {
      "audio_tokens": null,
      "cached_tokens": 0
    },
    "cache_creation_input_tokens": 0,
    "cache_read_input_tokens": 0
  }
}
```

#### Streaming

```
POST /api/v1/chat/completions/stream
Content-Type: application/json
Accept: text/event-stream
```

Returns Server-Sent Events (SSE) with chunked responses.

---

### Mistral OCR

```
POST /api/v1/mistral/ocr
Content-Type: application/json
```

**Request:**
```json
{
  "model": "mistral-ocr-2505",
  "document": {
    "type": "document_url",
    "document_url": "data:application/jpg;base64,{base64_encoded_document}"
  },
  "pages": "0"
}
```

**Successful Response:**
```json
{
  "pages": [
    {
      "index": 0,
      "markdown": "SMALL STEPS ARE STILL PROGRESS",
      "images": [],
      "dimensions": {
        "dpi": 200,
        "height": 1024,
        "width": 1024
      }
    }
  ],
  "model": "mistral-ocr-2505",
  "document_annotation": null,
  "usage_info": {
    "pages_processed": 1,
    "doc_size_bytes": 166364
  }
}
```

**Error Response:**
```json
{
  "message": {
    "error_subject": "OCR Error",
    "error_message": "Invalid base64 document format or unsupported file type."
  }
}
```

---

### Health Endpoints

```
GET /api/v1/chat/health
GET /api/v1/mistral/ocr/health
GET /actuator/health
```

## Building and Running

### Build

```bash
mvn clean package
```

### Run

```bash
# With default profile
java -jar target/llm-passthrough-service-1.0.0.jar

# With local profile (SSL disabled)
java -jar target/llm-passthrough-service-1.0.0.jar --spring.profiles.active=local
```

### Run with Maven

```bash
mvn spring-boot:run
```

## SSL/TLS Configuration

The service supports two certificate formats:

### PEM Certificates (Recommended)

Place your certificates in `src/main/resources/certs/`:
- `tls.key` - Private key
- `tls.crt` - Client certificate
- `ca.crt` - CA certificate

### Java KeyStore

Alternatively, use JKS format:

```yaml
apigee:
  ssl:
    enabled: true
    key-store-path: classpath:certs/keystore.jks
    key-store-password: ${KEYSTORE_PASSWORD}
    trust-store-path: classpath:certs/truststore.jks
    trust-store-password: ${TRUSTSTORE_PASSWORD}
```

## Project Structure

```
src/main/java/com/llm/passthrough/
├── LlmPassthroughApplication.java    # Application entry point
├── config/
│   ├── ApigeeProperties.java         # Configuration properties
│   └── SslConfig.java                # SSL and RestClient configuration
├── controller/
│   ├── ChatController.java           # Chat completions endpoints
│   └── OcrController.java            # OCR endpoints
├── service/
│   ├── LlmService.java               # Chat service logic
│   └── OcrService.java               # OCR service logic
├── dto/
│   ├── ChatRequest.java              # Chat request DTO
│   ├── ChatResponse.java             # Chat response DTO
│   ├── Message.java                  # Message DTO (supports multimodal)
│   ├── ContentPart.java              # Multimodal content part DTO
│   ├── OcrRequest.java               # OCR request DTO
│   ├── OcrResponse.java              # OCR response DTO
│   └── ...                           # Other DTOs
└── exception/
    ├── ApigeeException.java          # Custom exception
    └── GlobalExceptionHandler.java   # Global error handling
```

## Error Handling

All errors return a consistent format:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Validation Error",
  "message": "model: Model is required",
  "path": "/api/v1/chat/completions"
}
```

## License

Proprietary - Internal Use Only
