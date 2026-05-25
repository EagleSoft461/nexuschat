package com.nexuschat.dto.request;

import com.nexuschat.model.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotBlank(message = "Content is required")
    private String content;

    private Message.MessageType type = Message.MessageType.TEXT;
}
