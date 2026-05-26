package com.nexuschat.controller;

import com.nexuschat.dto.request.CreateDmRequest;
import com.nexuschat.dto.request.CreateRoomRequest;
import com.nexuschat.dto.request.InviteUserRequest;
import com.nexuschat.dto.response.RoomResponse;
import com.nexuschat.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(roomService.createRoom(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getPublicRooms() {
        return ResponseEntity.ok(roomService.getPublicRooms());
    }

    @GetMapping("/my")
    public ResponseEntity<List<RoomResponse>> getMyRooms(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(roomService.getUserRooms(userDetails.getUsername()));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<Void> joinRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        roomService.joinRoom(roomId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        roomService.leaveRoom(roomId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /** Invite a user to a PRIVATE room (OWNER/ADMIN only) */
    @PostMapping("/{roomId}/invite")
    public ResponseEntity<Void> inviteUser(
            @PathVariable Long roomId,
            @Valid @RequestBody InviteUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        roomService.inviteUser(roomId, userDetails.getUsername(), request.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        roomService.deleteRoom(roomId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /** Create or retrieve a Direct Message room with another user */
    @PostMapping("/dm")
    public ResponseEntity<RoomResponse> createOrGetDm(
            @Valid @RequestBody CreateDmRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                roomService.createOrGetDm(userDetails.getUsername(), request.getTargetUsername()));
    }
}
