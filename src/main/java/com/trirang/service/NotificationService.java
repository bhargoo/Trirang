package com.trirang.service;

import com.trirang.model.dto.response.NotificationResponse;
import com.trirang.model.entity.Notification;
import com.trirang.model.entity.User;
import com.trirang.model.enums.shared.NotificationType;
import com.trirang.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void sendNotification(User user, String title, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .build();

        Notification saved = notificationRepository.save(notification);
        
        NotificationResponse response = NotificationResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .message(saved.getMessage())
                .type(saved.getType())
                .isRead(saved.getIsRead())
                .createdAt(saved.getCreatedAt())
                .build();

        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications",
                response
        );
        log.info("Sent {} notification to user {}", type, user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notif -> NotificationResponse.builder()
                        .id(notif.getId())
                        .title(notif.getTitle())
                        .message(notif.getMessage())
                        .type(notif.getType())
                        .isRead(notif.getIsRead())
                        .createdAt(notif.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId)
                .filter(notif -> notif.getUser().getId().equals(userId))
                .ifPresent(notif -> {
                    notif.setIsRead(true);
                    notificationRepository.save(notif);
                });
    }
}
