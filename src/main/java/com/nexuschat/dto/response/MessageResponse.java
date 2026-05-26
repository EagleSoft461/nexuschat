package com.nexuschat.dto.response;

import com.nexuschat.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private String content;
    private Message.MessageType type;
    private Long roomId;
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private boolean edited;
    private String fileUrl;
    private String fileName;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.isDeleted() ? "[Message deleted]" : message.getContent())
                .type(message.getType())
                .roomId(message.getRoom().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .senderDisplayName(message.getSender().getDisplayName())
                .edited(message.isEdited())
                .fileUrl(message.isDeleted() ? null : message.getFileUrl())
                .fileName(message.isDeleted() ? null : message.getFileName())
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .build();
    }
}
