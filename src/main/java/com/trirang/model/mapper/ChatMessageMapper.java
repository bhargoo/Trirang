package com.trirang.model.mapper;

import com.trirang.model.dto.ChatMessageResponse;
import com.trirang.model.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(target = "senderName", ignore = true)
    ChatMessageResponse toResponse(ChatMessage message);
}
