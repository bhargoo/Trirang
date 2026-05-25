package com.trirang.service;

import com.trirang.model.dto.*;
import com.trirang.model.entity.ChatMessage;
import com.trirang.model.entity.Match;
import com.trirang.model.entity.User;
import com.trirang.model.enums.MatchStatus;
import com.trirang.model.enums.Role;
import com.trirang.model.mapper.ChatMessageMapper;
import com.trirang.repository.ChatMessageRepository;
import com.trirang.repository.MatchRepository;
import com.trirang.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class ChatMessageService {

    private final ChatMessageRepository chatRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ChatMessageMapper chatMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageService(
            ChatMessageRepository chatRepository,
            MatchRepository matchRepository,
            UserRepository userRepository,
            ChatMessageMapper chatMapper,
            SimpMessagingTemplate messagingTemplate) {
        this.chatRepository = chatRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.chatMapper = chatMapper;
        this.messagingTemplate = messagingTemplate;
    }

    public ChatMessageResponse sendMessage(UUID matchId, User sender, SendMessageRequest request) {
        log.info("Sender {} sending chat message to match ID: {}", sender.getId(), matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        // Security check
        validateAccess(match, sender);

        // Verify status is ACCEPTED
        if (match.getStatus() != MatchStatus.ACCEPTED) {
            throw new IllegalStateException("Chatting is only allowed on ACCEPTED matches");
        }

        // Sanitize input
        String sanitizedMessage = HtmlUtils.htmlEscape(request.message().trim());
        if (sanitizedMessage.isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }

        ChatMessage message = ChatMessage.builder()
                .matchId(matchId)
                .senderId(sender.getId())
                .message(sanitizedMessage)
                .createdAt(Instant.now())
                .build();

        ChatMessage saved = chatRepository.save(message);
        
        // Map to response
        ChatMessageResponse response = mapToResponse(saved, sender.getFullName());

        // WebSocket realtime broadcast
        String destination = "/topic/matches/" + matchId + "/chat";
        messagingTemplate.convertAndSend(destination, response);
        log.info("Chat message broadcasted successfully to topic {}", destination);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(UUID matchId, User user, Pageable pageable) {
        log.info("Fetching paged chat messages for match ID: {} by user: {}", matchId, user.getId());

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        // Security check
        validateAccess(match, user);

        // Fetch messages
        Page<ChatMessage> messages = chatRepository.findByMatchId(matchId, pageable);

        return messages.map(msg -> {
            String senderName = userRepository.findById(msg.getSenderId())
                    .map(User::getFullName)
                    .orElse("Unknown User");
            return mapToResponse(msg, senderName);
        });
    }

    private void validateAccess(Match match, User user) {
        if (Role.ADMIN.name().equals(user.getRole())) {
            return;
        }

        boolean isDonor = match.getDonor().getId().equals(user.getId());
        boolean isArtisan = match.getArtisan().getId().equals(user.getId());

        if (!isDonor && !isArtisan) {
            throw new IllegalStateException("You are not authorized to access this match chat channel");
        }
    }

    private ChatMessageResponse mapToResponse(ChatMessage msg, String senderName) {
        return new ChatMessageResponse(
                msg.getId(),
                msg.getMatchId(),
                msg.getSenderId(),
                senderName,
                msg.getMessage(),
                msg.getCreatedAt()
        );
    }
}
