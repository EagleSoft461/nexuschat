package com.nexuschat.service;

import com.nexuschat.dto.request.EditMessageRequest;
import com.nexuschat.dto.request.SendMessageRequest;
import com.nexuschat.dto.response.MessageResponse;
import com.nexuschat.dto.response.UnreadCountResponse;
import com.nexuschat.model.Message;
import com.nexuschat.model.Room;
import com.nexuschat.model.RoomMember;
import com.nexuschat.model.User;
import com.nexuschat.repository.MessageRepository;
import com.nexuschat.repository.RoomMemberRepository;
import com.nexuschat.repository.RoomRepository;
import com.nexuschat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

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

        // Validate: TEXT must have content, FILE/IMAGE must have fileUrl
        if (request.getType() == Message.MessageType.TEXT
                && (request.getContent() == null || request.getContent().isBlank())) {
            throw new IllegalArgumentException("Content is required for text messages");
        }
        if ((request.getType() == Message.MessageType.FILE
                || request.getType() == Message.MessageType.IMAGE)
                && (request.getFileUrl() == null || request.getFileUrl().isBlank())) {
            throw new IllegalArgumentException("fileUrl is required for file/image messages");
        }

        Message message = Message.builder()
                .content(request.getContent() != null ? request.getContent() : "")
                .type(request.getType())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .sender(sender)
                .room(room)
                .build();

        message = messageRepository.save(message);
        MessageResponse response = MessageResponse.from(message);

        log.info("Publishing WebSocket message: roomId={}, messageId={}, sender={}",
                room.getId(), response.getId(), username);

        // Publish to Redis for real-time delivery
        redisMessagePublisher.publishMessage("chat:" + room.getId(), response);

        log.info("Redis publish completed: channel=chat:{}, messageId={}",
                room.getId(), response.getId());

        return response;

    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getRoomMessages(Long roomId, int page, int size, String username) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!roomMemberRepository.existsByRoomAndUser(room, user)) {
            throw new IllegalStateException("You are not a member of this room");
        }

        Page<Message> messages = messageRepository.findByRoomAndDeletedFalseOrderByCreatedAtDesc(
                room, PageRequest.of(page, size));

        return messages.getContent()
                .stream()
                .map(m -> MessageResponse.from(m, username))
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse editMessage(Long messageId, EditMessageRequest request, String username) {
        Message message = messageRepository.findByIdWithSender(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (!message.getSender().getUsername().equals(username)) {
            throw new IllegalStateException("You can only edit your own messages");
        }
        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted message");
        }

        message.setContent(request.getContent());
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        message = messageRepository.save(message);

        MessageResponse response = MessageResponse.from(message);
        // Broadcast the edit event via Redis
        redisMessagePublisher.publishMessage("chat:" + message.getRoom().getId(), response);
        return response;
    }

    @Transactional
    public void deleteMessage(Long messageId, String username) {
        Message message = messageRepository.findByIdWithSender(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (!message.getSender().getUsername().equals(username)) {
            throw new IllegalStateException("You can only delete your own messages");
        }

        message.setDeleted(true);
        message = messageRepository.save(message);

        // Broadcast deletion to all room members via Redis
        MessageResponse response = MessageResponse.from(message);
        redisMessagePublisher.publishMessage("chat:" + message.getRoom().getId(), response);
    }

    /**
     * Hide a message only for the requesting user (local delete).
     * The message remains visible to others.
     */
    @Transactional
    public void deleteMessageForMe(Long messageId, String username) {
        Message message = messageRepository.findByIdWithSender(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        // Any room member can hide a message for themselves
        if (message.getHiddenBy() == null) {
            message.setHiddenBy(new java.util.HashSet<>());
        }
        message.getHiddenBy().add(username);
        messageRepository.save(message);
        // No broadcast — only affects the requesting user's view
    }

    /**
     * Mark all messages up to (and including) the given messageId as read for this user.
     */
    @Transactional
    public void markAsRead(Long roomId, Long messageId, String username) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        RoomMember member = roomMemberRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new IllegalStateException("You are not a member of this room"));

        // Only advance the pointer, never go backwards
        if (member.getLastReadMessageId() == null || messageId > member.getLastReadMessageId()) {
            member.setLastReadMessageId(messageId);
            roomMemberRepository.save(member);
        }
    }

    /**
     * Returns the number of unread messages in a room for the given user.
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long roomId, String username) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        RoomMember member = roomMemberRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new IllegalStateException("You are not a member of this room"));

        long unread;
        if (member.getLastReadMessageId() == null) {
            unread = messageRepository.countByRoomAndDeletedFalse(room);
        } else {
            unread = messageRepository.countUnreadMessages(room, member.getLastReadMessageId());
        }

        return new UnreadCountResponse(roomId, unread);
    }

    /**
     * Returns unread counts for all rooms the user is a member of.
     */
    @Transactional(readOnly = true)
    public List<UnreadCountResponse> getAllUnreadCounts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return roomMemberRepository.findByUser(user).stream()
                .map(member -> {
                    long unread;
                    if (member.getLastReadMessageId() == null) {
                        unread = messageRepository.countByRoomAndDeletedFalse(member.getRoom());
                    } else {
                        unread = messageRepository.countUnreadMessages(
                                member.getRoom(), member.getLastReadMessageId());
                    }
                    return new UnreadCountResponse(member.getRoom().getId(), unread);
                })
                .collect(Collectors.toList());
    }

    /**
     * Cursor-based pagination for messages.
     * If cursor is null, returns the latest messages.
     * If cursor is provided, returns messages older than the cursor.
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getRoomMessagesCursor(Long roomId, Long cursor, int limit, String username) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!roomMemberRepository.existsByRoomAndUser(room, user)) {
            throw new IllegalStateException("You are not a member of this room");
        }

        Page<Message> messages;
        if (cursor == null) {
            // No cursor: return latest messages
            messages = messageRepository.findByRoomAndDeletedFalseOrderByCreatedAtDesc(
                    room, PageRequest.of(0, limit));
        } else {
            // With cursor: return messages before cursor (older)
            messages = messageRepository.findByRoomBeforeCursor(
                    room, cursor, PageRequest.of(0, limit));
        }

        return messages.getContent()
                .stream()
                .map(m -> MessageResponse.from(m, username))
                .collect(Collectors.toList());
    }
}
