package com.nexuschat.dto.request;

import com.nexuschat.model.Message;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "Room ID is required")
    private Long roomId;

    // content is optional for FILE/IMAGE messages (caption)
    private String content = "";

    private Message.MessageType type = Message.MessageType.TEXT;

    // For IMAGE / FILE type messages
    private String fileUrl;
    private String fileName;
}
