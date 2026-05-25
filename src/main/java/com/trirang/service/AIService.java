package com.trirang.service;

import com.trirang.ai.GroqClient;
import com.trirang.model.dto.AiClassificationResponse;
import com.trirang.model.enums.shared.Classification;
import com.trirang.model.enums.shared.FabricType;
import com.trirang.model.enums.shared.ItemCategory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class AIService {

    private final GroqClient groqClient;

    public AIService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    @CircuitBreaker(name = "groqService", fallbackMethod = "fallbackClassify")
    @Retry(name = "groqService")
    public AiClassificationResponse classify(String base64Image) {
        log.info("Attempting AI image classification via GroqClient");
        return groqClient.classifyImage(base64Image);
    }

    /**
     * Fallback method invoked by Resilience4j when the main Groq call fails or the circuit is open.
     *
     * @param base64Image the base64 image data string
     * @param throwable   the exception that triggered the fallback
     * @return a safe fallback classification response
     */
    public AiClassificationResponse fallbackClassify(String base64Image, Throwable throwable) {
        log.error("AI image classification failed. Invoking Resilience4j fallback. Reason: {}", throwable.getMessage());

        return new AiClassificationResponse(
                ItemCategory.OTHER,
                FabricType.OTHER,
                Classification.OTHER,
                "UNKNOWN",
                BigDecimal.ZERO,
                "Classification failed. Fallback generated due to error: " + throwable.getMessage(),
                "RECYCLE"
        );
    }
}
