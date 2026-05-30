package com.nexuschat.service;

import com.nexuschat.dto.response.AdminStatsResponse;
import com.nexuschat.dto.response.RoomResponse;
import com.nexuschat.dto.response.UserResponse;
import com.nexuschat.model.Room;
import com.nexuschat.model.User;
import com.nexuschat.repository.MessageRepository;
import com.nexuschat.repository.RoomRepository;
import com.nexuschat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MessageRepository messageRepository;

    /**
     * Get platform statistics
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalRooms = roomRepository.count();
        long totalMessages = messageRepository.count();
        long publicRooms = roomRepository.countByType(Room.RoomType.PUBLIC);
        long privateRooms = roomRepository.countByType(Room.RoomType.PRIVATE);
        long directMessages = roomRepository.countByType(Room.RoomType.DIRECT);

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalRooms(totalRooms)
                .totalMessages(totalMessages)
                .activeUsers(totalUsers) // TODO: implement active users tracking
                .publicRooms(publicRooms)
                .privateRooms(privateRooms)
                .directMessages(directMessages)
                .build();
    }

    /**
     * Get all users with pagination
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        return users.getContent().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get all rooms with pagination
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms(int page, int size) {
        Page<Room> rooms = roomRepository.findAll(PageRequest.of(page, size));
        return rooms.getContent().stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Delete a user (admin action)
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        userRepository.delete(user);
    }

    /**
     * Delete a room (admin action)
     */
    @Transactional
    public void deleteRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        roomRepository.delete(room);
    }

    /**
     * Ban/unban user (placeholder - implement user enabled/disabled field)
     */
    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        // TODO: Add enabled field to User model and toggle it
        userRepository.save(user);
    }
}
