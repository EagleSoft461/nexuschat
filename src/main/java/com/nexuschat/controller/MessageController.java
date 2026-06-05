package com.nexuschat.controller;

import com.nexuschat.dto.request.EditMessageRequest;
import com.nexuschat.dto.request.SendMessageRequest;
import com.nexuschat.dto.response.MessageResponse;
import com.nexuschat.dto.response.UnreadCountResponse;
import com.nexuschat.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messageService.sendMessage(request, userDetails.getUsername()));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<MessageResponse>> getRoomMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messageService.getRoomMessages(roomId, page, size, userDetails.getUsername()));
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messageService.editMessage(messageId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        messageService.deleteMessage(messageId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /** Hide a message only for the current user (does not affect others) */
    @DeleteMapping("/{messageId}/me")
    public ResponseEntity<Void> deleteMessageForMe(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        messageService.deleteMessageForMe(messageId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /** Mark messages as read up to the given messageId */
    @PostMapping("/room/{roomId}/read/{messageId}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        messageService.markAsRead(roomId, messageId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /** Get unread count for a specific room */
    @GetMapping("/room/{roomId}/unread")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messageService.getUnreadCount(roomId, userDetails.getUsername()));
    }

    /** Get unread counts for all rooms the user is a member of */
    @GetMapping("/unread")
    public ResponseEntity<List<UnreadCountResponse>> getAllUnreadCounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messageService.getAllUnreadCounts(userDetails.getUsername()));
    }

    /** Cursor-based pagination: get messages before a cursor (older messages) */
    @GetMapping("/room/{roomId}/cursor")
    public ResponseEntity<List<MessageResponse>> getRoomMessagesCursor(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messageService.getRoomMessagesCursor(roomId, cursor, limit, userDetails.getUsername()));
    }
}
