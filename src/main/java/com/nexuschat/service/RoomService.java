package com.nexuschat.service;

import com.nexuschat.dto.request.CreateRoomRequest;
import com.nexuschat.dto.response.RoomResponse;
import com.nexuschat.model.Room;
import com.nexuschat.model.RoomMember;
import com.nexuschat.model.User;
import com.nexuschat.repository.RoomMemberRepository;
import com.nexuschat.repository.RoomRepository;
import com.nexuschat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Room room = Room.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .createdBy(creator)
                .build();

        room = roomRepository.save(room);

        // Add creator as owner
        RoomMember member = RoomMember.builder()
                .room(room)
                .user(creator)
                .role(RoomMember.MemberRole.OWNER)
                .build();
        roomMemberRepository.save(member);

        // Reload with members fetched to avoid LazyInitializationException in RoomResponse.from()
        room = roomRepository.findByIdWithCreator(room.getId())
                .orElseThrow(() -> new IllegalStateException("Room not found after save"));

        return RoomResponse.from(room);
    }

    public List<RoomResponse> getPublicRooms() {
        return roomRepository.findByType(Room.RoomType.PUBLIC)
                .stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
    }

    public List<RoomResponse> getUserRooms(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return roomRepository.findRoomsByMember(user)
                .stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
    }

    public RoomResponse getRoomById(Long roomId) {
        Room room = roomRepository.findByIdWithCreator(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        return RoomResponse.from(room);
    }

    @Transactional
    public void joinRoom(Long roomId, String username) {
        Room room = roomRepository.findByIdWithCreator(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Zaten üyeyse sessizce geç
        if (roomMemberRepository.existsByRoomAndUser(room, user)) {
            return;
        }

        RoomMember member = RoomMember.builder()
                .room(room)
                .user(user)
                .role(RoomMember.MemberRole.MEMBER)
                .build();
        roomMemberRepository.save(member);
    }

    @Transactional
    public void leaveRoom(Long roomId, String username) {
        Room room = roomRepository.findByIdWithCreator(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        roomMemberRepository.deleteByRoomAndUser(room, user);
    }

    @Transactional
    public void deleteRoom(Long roomId, String username) {
        Room room = roomRepository.findByIdWithCreator(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        // Sadece oda sahibi silebilir
        if (!room.getCreatedBy().getUsername().equals(username)) {
            throw new IllegalStateException("Only the room owner can delete this room");
        }

        roomRepository.delete(room);
    }

    /**
     * Invite a user to a PRIVATE room. Only OWNER or ADMIN can invite.
     */
    @Transactional
    public void inviteUser(Long roomId, String inviterUsername, String targetUsername) {
        Room room = roomRepository.findByIdWithCreator(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.getType() == Room.RoomType.PUBLIC) {
            throw new IllegalStateException("Public rooms do not require invitations");
        }

        User inviter = userRepository.findByUsername(inviterUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + inviterUsername));

        RoomMember inviterMember = roomMemberRepository.findByRoomAndUser(room, inviter)
                .orElseThrow(() -> new IllegalStateException("You are not a member of this room"));

        if (inviterMember.getRole() == RoomMember.MemberRole.MEMBER) {
            throw new IllegalStateException("Only OWNER or ADMIN can invite users");
        }

        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + targetUsername));

        if (roomMemberRepository.existsByRoomAndUser(room, target)) {
            return; // already a member — silently ignore
        }

        RoomMember newMember = RoomMember.builder()
                .room(room)
                .user(target)
                .role(RoomMember.MemberRole.MEMBER)
                .build();
        roomMemberRepository.save(newMember);
    }

    /**
     * Create or retrieve an existing Direct Message room between two users.
     */
    @Transactional
    public RoomResponse createOrGetDm(String requesterUsername, String targetUsername) {
        if (requesterUsername.equals(targetUsername)) {
            throw new IllegalArgumentException("Cannot create a DM with yourself");
        }

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + requesterUsername));
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + targetUsername));

        // Return existing DM if it already exists
        return roomRepository.findDirectMessageRoom(requester, target)
                .map(RoomResponse::from)
                .orElseGet(() -> {
                    // DM room name is deterministic: sorted usernames joined with ":"
                    String dmName = java.util.stream.Stream.of(requesterUsername, targetUsername)
                            .sorted()
                            .collect(java.util.stream.Collectors.joining(":"));

                    Room room = Room.builder()
                            .name(dmName)
                            .type(Room.RoomType.DIRECT)
                            .createdBy(requester)
                            .build();
                    room = roomRepository.save(room);

                    roomMemberRepository.save(RoomMember.builder()
                            .room(room).user(requester).role(RoomMember.MemberRole.OWNER).build());
                    roomMemberRepository.save(RoomMember.builder()
                            .room(room).user(target).role(RoomMember.MemberRole.MEMBER).build());

                    room = roomRepository.findByIdWithCreator(room.getId())
                            .orElseThrow(() -> new IllegalStateException("DM room not found after save"));
                    return RoomResponse.from(room);
                });
    }
}
