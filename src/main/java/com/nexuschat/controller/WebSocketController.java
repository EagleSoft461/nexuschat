package com.nexuschat.controller;

import com.nexuschat.dto.request.EditMessageRequest;
import com.nexuschat.dto.request.SendMessageRequest;
import com.nexuschat.service.MessageService;
import com.nexuschat.service.PresenceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Controller
public class WebSocketController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload SendMessageRequest request, Principal principal) {
        if (principal == null) return;
        messageService.sendMessage(request, principal.getName());
    }

    @MessageMapping("/chat.edit")
    public void editMessage(@Valid @Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;
        Object msgIdObj = payload.get("messageId");
        Object contentObj = payload.get("content");
        if (msgIdObj == null || contentObj == null) return;
        try {
            Long messageId = Long.valueOf(msgIdObj.toString());
            EditMessageRequest req = new EditMessageRequest();
            req.setContent(contentObj.toString());
            messageService.editMessage(messageId, req, principal.getName());
        } catch (NumberFormatException ignored) {}
    }

    @MessageMapping("/chat.read")
    public void markRead(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;
        Object roomIdObj = payload.get("roomId");
        Object msgIdObj = payload.get("messageId");
        if (roomIdObj == null || msgIdObj == null) return;
        try {
            Long roomId = Long.valueOf(roomIdObj.toString());
            Long messageId = Long.valueOf(msgIdObj.toString());
            messageService.markAsRead(roomId, messageId, principal.getName());

            // Broadcast read receipt to room so others can see it
            Map<String, Object> event = new HashMap<>();
            event.put("username", principal.getName());
            event.put("messageId", messageId);
            event.put("roomId", roomId);
            messagingTemplate.convertAndSend("/topic/room." + roomId + ".read", event);
        } catch (NumberFormatException ignored) {}
    }

    @MessageMapping("/presence.ping")
    public void presencePing(Principal principal) {
        if (principal == null) return;
        presenceService.refreshPresence(principal.getName());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;
        Object roomIdObj = payload.get("roomId");
        if (roomIdObj == null) return;
        try {
            Long roomId = Long.valueOf(roomIdObj.toString());
            boolean isTyping = Boolean.parseBoolean(payload.getOrDefault("typing", true).toString());
            Map<String, Object> event = new HashMap<>();
            event.put("username", principal.getName());
            event.put("typing", isTyping);
            event.put("roomId", roomId);
            messagingTemplate.convertAndSend("/topic/room." + roomId + ".typing", event);
        } catch (NumberFormatException ignored) {}
    }

    @MessageMapping("/presence.list")
    @SendToUser("/queue/presence.list")
    public Set<String> getOnlineUsers(SimpMessageHeaderAccessor headerAccessor) {
        return presenceService.getOnlineUsers();
    }
}
