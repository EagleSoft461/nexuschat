package com.nexuschat.controller;

import com.nexuschat.dto.response.MessageResponse;
import com.nexuschat.service.MessageSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Message search endpoints")
public class SearchController {

    @Autowired
    private MessageSearchService searchService;

    @GetMapping("/messages")
    @Operation(summary = "Search messages globally")
    public ResponseEntity<List<MessageResponse>> searchMessages(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(searchService.searchMessages(query, page, size));
    }

    @GetMapping("/messages/room/{roomId}")
    @Operation(summary = "Search messages in a specific room")
    public ResponseEntity<List<MessageResponse>> searchMessagesInRoom(
            @PathVariable Long roomId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(searchService.searchMessagesInRoom(roomId, query, page, size));
    }

    @GetMapping("/messages/sender/{username}")
    @Operation(summary = "Search messages by sender")
    public ResponseEntity<List<MessageResponse>> searchMessagesBySender(
            @PathVariable String username,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(searchService.searchMessagesBySender(username, query, page, size));
    }
}
