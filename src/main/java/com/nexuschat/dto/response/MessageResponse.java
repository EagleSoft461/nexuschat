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
    private boolean deletedForEveryone;
    private String fileUrl;
    private String fileName;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;

    public static MessageResponse from(Message message) {
        return from(message, null);
    }

    public static MessageResponse from(Message message, String viewerUsername) {
        boolean deletedForEveryone = message.isDeleted();
        boolean hiddenForMe = viewerUsername != null
                && message.getHiddenBy() != null
                && message.getHiddenBy().contains(viewerUsername);

        String content = (deletedForEveryone || hiddenForMe)
                ? "[Message deleted]"
                : message.getContent();

        return MessageResponse.builder()
                .id(message.getId())
                .content(content)
                .type(message.getType())
                .roomId(message.getRoom().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .senderDisplayName(message.getSender().getDisplayName())
                .edited(message.isEdited())
                .deletedForEveryone(deletedForEveryone)
                .fileUrl((deletedForEveryone || hiddenForMe) ? null : message.getFileUrl())
                .fileName((deletedForEveryone || hiddenForMe) ? null : message.getFileName())
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .build();
    }
}
