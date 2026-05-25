package com.trirang.controller;

import com.trirang.model.dto.AiClassificationRequest;
import com.trirang.model.dto.AiClassificationResponse;
import com.trirang.service.AIService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/classify")
    public ResponseEntity<AiClassificationResponse> classify(
            @Valid @RequestBody AiClassificationRequest request) {
        log.info("Received request to classify textile image");
        AiClassificationResponse response = aiService.classify(request.base64Image());
        return ResponseEntity.ok(response);
    }
}
