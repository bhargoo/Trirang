package com.trirang.controller;

import com.trirang.model.dto.*;
import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import com.trirang.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/matches/{id}/messages")
public class ChatMessageController {

    private final ChatMessageService chatService;
    private final UserRepository userRepository;

    public ChatMessageController(ChatMessageService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SendMessageRequest request) {

        log.info("Received request to send chat message for match ID: {}", id);
        User currentUser = getCurrentUser();
        ChatMessageResponse response = chatService.sendMessage(id, currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ChatMessageResponse>> getMessages(
            @PathVariable("id") UUID id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {

        log.info("Received request to fetch chat messages history for match ID: {}", id);
        User currentUser = getCurrentUser();
        
        // Chat messages are best ordered descending by creation date so the newest load first in pagination!
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<ChatMessageResponse> responses = chatService.getMessages(id, currentUser, pageable);
        return ResponseEntity.ok(responses);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found in database"));
    }
}
