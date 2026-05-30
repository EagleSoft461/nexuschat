package com.nexuschat.service;

import com.nexuschat.dto.request.EditMessageRequest;
import com.nexuschat.dto.request.SendMessageRequest;
import com.nexuschat.dto.response.MessageResponse;
import com.nexuschat.model.Message;
import com.nexuschat.model.Room;
import com.nexuschat.model.RoomMember;
import com.nexuschat.model.User;
import com.nexuschat.repository.MessageRepository;
import com.nexuschat.repository.RoomMemberRepository;
import com.nexuschat.repository.RoomRepository;
import com.nexuschat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Unit Tests")
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoomMemberRepository roomMemberRepository;
    @Mock private RedisMessagePublisher redisMessagePublisher;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private Room room;
    private Message message;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L).username("alice").email("alice@nexus.chat")
                .displayName("Alice").password("encoded").build();

        room = Room.builder()
                .id(10L).name("general").type(Room.RoomType.PUBLIC)
                .createdBy(sender).build();

        message = Message.builder()
                .id(100L).content("Hello!").type(Message.MessageType.TEXT)
                .sender(sender).room(room).hiddenBy(new HashSet<>()).build();
    }

    // ── sendMessage ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendMessage: success — saves and publishes to Redis")
    void sendMessage_success() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(10L);
        request.setContent("Hello!");
        request.setType(Message.MessageType.TEXT);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomAndUser(room, sender)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        MessageResponse response = messageService.sendMessage(request, "alice");

        assertThat(response.getContent()).isEqualTo("Hello!");
        assertThat(response.getSenderUsername()).isEqualTo("alice");
        assertThat(response.getRoomId()).isEqualTo(10L);
        verify(messageRepository).save(any(Message.class));
        verify(redisMessagePublisher).publishMessage(eq("chat:10"), any());
    }

    @Test
    @DisplayName("sendMessage: throws when user is not a room member")
    void sendMessage_throwsWhenNotMember() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(10L);
        request.setContent("Hello!");
        request.setType(Message.MessageType.TEXT);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomAndUser(room, sender)).thenReturn(false);

        assertThatThrownBy(() -> messageService.sendMessage(request, "alice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a member");

        verify(messageRepository, never()).save(any());
        verify(redisMessagePublisher, never()).publishMessage(any(), any());
    }

    @Test
    @DisplayName("sendMessage: throws when TEXT message has blank content")
    void sendMessage_throwsWhenTextContentBlank() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(10L);
        request.setContent("   ");
        request.setType(Message.MessageType.TEXT);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomAndUser(room, sender)).thenReturn(true);

        assertThatThrownBy(() -> messageService.sendMessage(request, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content is required");
    }

    @Test
    @DisplayName("sendMessage: throws when FILE message has no fileUrl")
    void sendMessage_throwsWhenFileUrlMissing() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(10L);
        request.setContent("attachment");
        request.setType(Message.MessageType.FILE);
        // fileUrl not set

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomAndUser(room, sender)).thenReturn(true);

        assertThatThrownBy(() -> messageService.sendMessage(request, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileUrl is required");
    }

    // ── editMessage ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("editMessage: success — updates content and broadcasts")
    void editMessage_success() {
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("Updated content");

        when(messageRepository.findByIdWithSender(100L)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        MessageResponse response = messageService.editMessage(100L, request, "alice");

        assertThat(message.isEdited()).isTrue();
        assertThat(message.getContent()).isEqualTo("Updated content");
        verify(redisMessagePublisher).publishMessage(eq("chat:10"), any());
    }

    @Test
    @DisplayName("editMessage: throws when user is not the sender")
    void editMessage_throwsWhenNotSender() {
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("Hacked content");

        when(messageRepository.findByIdWithSender(100L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.editMessage(100L, request, "bob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only edit your own");

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("editMessage: throws when message is already deleted")
    void editMessage_throwsWhenDeleted() {
        message.setDeleted(true);
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("New content");

        when(messageRepository.findByIdWithSender(100L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.editMessage(100L, request, "alice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot edit a deleted");
    }

    // ── deleteMessage ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteMessage: success — sets deleted flag and broadcasts")
    void deleteMessage_success() {
        when(messageRepository.findByIdWithSender(100L)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        messageService.deleteMessage(100L, "alice");

        assertThat(message.isDeleted()).isTrue();
        verify(redisMessagePublisher).publishMessage(eq("chat:10"), any());
    }

    @Test
    @DisplayName("deleteMessage: throws when user is not the sender")
    void deleteMessage_throwsWhenNotSender() {
        when(messageRepository.findByIdWithSender(100L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.deleteMessage(100L, "bob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only delete your own");

        assertThat(message.isDeleted()).isFalse();
        verify(redisMessagePublisher, never()).publishMessage(any(), any());
    }

    // ── deleteMessageForMe ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteMessageForMe: adds username to hiddenBy, no broadcast")
    void deleteMessageForMe_success() {
        when(messageRepository.findByIdWithSender(100L)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        messageService.deleteMessageForMe(100L, "alice");

        assertThat(message.getHiddenBy()).contains("alice");
        assertThat(message.isDeleted()).isFalse(); // not globally deleted
        verify(redisMessagePublisher, never()).publishMessage(any(), any());
    }

    // ── getRoomMessages ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getRoomMessages: returns messages for room member")
    void getRoomMessages_success() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(roomMemberRepository.existsByRoomAndUser(room, sender)).thenReturn(true);
        when(messageRepository.findByRoomAndDeletedFalseOrderByCreatedAtDesc(eq(room), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(message)));

        List<MessageResponse> result = messageService.getRoomMessages(10L, 0, 50, "alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Hello!");
    }

    @Test
    @DisplayName("getRoomMessages: throws when user is not a member")
    void getRoomMessages_throwsWhenNotMember() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(
                User.builder().id(2L).username("bob").email("bob@nexus.chat").build()));
        when(roomMemberRepository.existsByRoomAndUser(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> messageService.getRoomMessages(10L, 0, 50, "bob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a member");
    }
}
