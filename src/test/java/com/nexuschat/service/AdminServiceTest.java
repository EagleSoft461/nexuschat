package com.nexuschat.service;

import com.nexuschat.model.User;
import com.nexuschat.repository.MessageRepository;
import com.nexuschat.repository.RoomRepository;
import com.nexuschat.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private PresenceService presenceService;

    @InjectMocks
    private AdminService adminService;

    @Test
    void toggleUserStatus_shouldFlipActiveFlag() {
        User user = User.builder().id(1L).username("bob").active(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminService.toggleUserStatus(1L);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void toggleUserStatus_unknownUser_shouldThrow() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.toggleUserStatus(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
