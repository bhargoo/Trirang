package com.trirang.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    public void sendProgressNotification(UUID requestId, String message) {
        log.info("[NOTIFICATION] Transformation request ID: {}: {}", requestId, message);
    }
}
