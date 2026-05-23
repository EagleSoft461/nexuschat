package com.nexuschat.service;

import com.nexuschat.dto.request.SendMessageRequest;
import com.nexuschat.dto.response.MessageResponse;
import com.nexuschat.model.Message;
import com.nexuschat.model.Room;
import com.nexuschat.model.User;
import com.nexuschat.repository.MessageRepository;
import com.nexuschat.repository.RoomMemberRepository;
import com.nexuschat.repository.RoomRepository;
import com.nexuschat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private RedisMessagePublisher redisMessagePublisher;

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request, String username) {
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + request.getRoomId()));

        if (!roomMemberRepository.existsByRoomAndUser(room, sender)) {
            throw new IllegalStateException("User is not a member of this room");
        }

        Message message = Message.builder()
                .content(request.getContent())
                .type(request.getType())
                .sender(sender)
                .room(room)
                .build();

        message = messageRepository.save(message);
        MessageResponse response = MessageResponse.from(message);

        // Publish to Redis for real-time delivery
        redisMessagePublisher.publishMessage("chat:" + room.getId(), response);

        return response;
    }

    public List<MessageResponse> getRoomMessages(Long roomId, int page, int size) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        Page<Message> messages = messageRepository.findByRoomAndDeletedFalseOrderByCreatedAtDesc(
                room, PageRequest.of(page, size));

        return messages.getContent()
                .stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMessage(Long messageId, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (!message.getSender().getUsername().equals(username)) {
            throw new IllegalStateException("You can only delete your own messages");
        }

        message.setDeleted(true);
        messageRepository.save(message);
    }
}
