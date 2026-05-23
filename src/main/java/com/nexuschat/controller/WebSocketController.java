package com.nexuschat.controller;

import com.nexuschat.dto.request.SendMessageRequest;
import com.nexuschat.dto.response.MessageResponse;
import com.nexuschat.service.MessageService;
import com.nexuschat.service.PresenceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Set;

@Controller
public class WebSocketController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private PresenceService presenceService;

    /**
     * Handles messages sent to /app/chat.send
     * The message is published to Redis and then broadcast to /topic/room.{roomId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload SendMessageRequest request, Principal principal) {
        messageService.sendMessage(request, principal.getName());
    }

    /**
     * Handles presence ping sent to /app/presence.ping
     * Refreshes the user's online TTL in Redis
     */
    @MessageMapping("/presence.ping")
    public void presencePing(Principal principal) {
        presenceService.refreshPresence(principal.getName());
    }

    /**
     * Handles request for online users sent to /app/presence.list
     */
    @MessageMapping("/presence.list")
    public Set<String> getOnlineUsers(SimpMessageHeaderAccessor headerAccessor) {
        return presenceService.getOnlineUsers();
    }
}
