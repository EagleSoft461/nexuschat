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
}
