package com.trirang.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trirang.model.dto.AiClassificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GroqClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String apiModel;

    private static final String SYSTEM_PROMPT = """
            You are an advanced AI specialized in textile classification and quality assessment.
            You must analyze the provided textile image and output a valid JSON object matching the following schema exactly:
            {
               "category": "APPAREL | FOOTWEAR | ACCESSORIES | HOME_TEXTILES | OTHER",
               "fabricType": "COTTON | SILK | WOOL | LINEN | DENIM | SYNTHETIC | OTHER",
               "classification": "CASUAL | FORMAL | SPORT | ETHNIC | OTHER",
               "condition": "POOR | FAIR | GOOD | EXCELLENT",
               "confidence": 0.95,
               "description": "Provide a concise visual description of the textile item, detailing color, style, patterns, and noticeable wear.",
               "suggestedAction": "REWEAR | REUSE | RECYCLE"
            }
            Rules:
            1. Respond ONLY with the raw JSON object. Do not include markdown wraps (like ```json), introduction, or explanations.
            2. Match enum values EXACTLY.
            """;

    public GroqClient(
            WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${groq.api.url}") String apiUrl,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.model}") String apiModel) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.apiModel = apiModel;
    }

    public AiClassificationResponse classifyImage(String base64Image) {
        // Enforce the standard data URI prefix required for Vision models
        String formattedImage = base64Image;
        if (!base64Image.startsWith("data:")) {
            formattedImage = "data:image/jpeg;base64," + base64Image;
        }

        Map<String, Object> requestBody = Map.of(
                "model", apiModel,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "type", "text",
                                                "text", SYSTEM_PROMPT
                                        ),
                                        Map.of(
                                                "type", "image_url",
                                                "image_url", Map.of(
                                                        "url", formattedImage
                                                )
                                        )
                                )
                        )
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.2
        );

        try {
            log.info("Sending request to Groq API to classify image using model: {}", apiModel);
            String responseBody = webClient.post()
                    .uri(apiUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Successfully received response from Groq API");

            JsonNode responseJson = objectMapper.readTree(responseBody);
            String rawContent = responseJson.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            log.debug("Raw AI Content response: {}", rawContent);

            // Deserialize the raw JSON response into the schema-compliant DTO
            return objectMapper.readValue(rawContent, AiClassificationResponse.class);

        } catch (Exception e) {
            log.error("Error communicating with Groq API or parsing response", e);
            throw new RuntimeException("Failed to process AI classification", e);
        }
    }
}
