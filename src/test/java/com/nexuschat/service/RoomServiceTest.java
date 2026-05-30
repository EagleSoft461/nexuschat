package com.nexuschat.service;

import com.nexuschat.dto.request.CreateRoomRequest;
import com.nexuschat.dto.response.RoomResponse;
import com.nexuschat.model.Room;
import com.nexuschat.model.RoomMember;
import com.nexuschat.model.User;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService Unit Tests")
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomMemberRepository roomMemberRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RoomService roomService;

    private User owner;
    private User member;
    private Room room;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L).username("alice").email("alice@nexus.chat")
                .displayName("Alice").password("encoded").build();

        member = User.builder()
                .id(2L).username("bob").email("bob@nexus.chat")
                .displayName("Bob").password("encoded").build();

        room = Room.builder()
                .id(10L).name("general").type(Room.RoomType.PUBLIC)
                .createdBy(owner).members(new ArrayList<>()).build();
    }

    // ── createRoom ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRoom: success — saves room and adds creator as OWNER")
    void createRoom_success() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("general");
        request.setType(Room.RoomType.PUBLIC);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMemberRepository.save(any(RoomMember.class))).thenReturn(
                RoomMember.builder().room(room).user(owner).role(RoomMember.MemberRole.OWNER).build());
        when(roomRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(room));

        RoomResponse response = roomService.createRoom(request, "alice");

        assertThat(response.getName()).isEqualTo("general");
        assertThat(response.getCreatedByUsername()).isEqualTo("alice");
        verify(roomRepository).save(any(Room.class));
        verify(roomMemberRepository).save(argThat(m ->
                m.getRole() == RoomMember.MemberRole.OWNER && m.getUser().equals(owner)));
    }

    @Test
    @DisplayName("createRoom: throws when user not found")
    void createRoom_throwsWhenUserNotFound() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("general");

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createRoom(request, "unknown"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);

        verify(roomRepository, never()).save(any());
    }

    // ── joinRoom ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("joinRoom: success — adds user as MEMBER")
    void joinRoom_success() {
        when(roomRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(room));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(member));
        when(roomMemberRepository.existsByRoomAndUser(room, member)).thenReturn(false);

        roomService.joinRoom(10L, "bob");

        verify(roomMemberRepository).save(argThat(m ->
                m.getRole() == RoomMember.MemberRole.MEMBER && m.getUser().equals(member)));
    }

    @Test
    @DisplayName("joinRoom: silently ignores when user is already a member")
    void joinRoom_silentWhenAlreadyMember() {
        when(roomRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(room));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(member));
        when(roomMemberRepository.existsByRoomAndUser(room, member)).thenReturn(true);

        roomService.joinRoom(10L, "bob");

        verify(roomMemberRepository, never()).save(any());
    }

    // ── deleteRoom ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteRoom: success — owner can delete")
    void deleteRoom_success() {
        when(roomRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(room));

        roomService.deleteRoom(10L, "alice");

        verify(roomRepository).delete(room);
    }

    @Test
    @DisplayName("deleteRoom: throws when non-owner tries to delete")
    void deleteRoom_throwsWhenNotOwner() {
        when(roomRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.deleteRoom(10L, "bob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the room owner");

        verify(roomRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteRoom: throws when room not found")
    void deleteRoom_throwsWhenRoomNotFound() {
        when(roomRepository.findByIdWithCreator(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.deleteRoom(99L, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Room not found");
    }

    // ── inviteUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("inviteUser: success — OWNER can invite to PRIVATE room")
    void inviteUser_success() {
        Room privateRoom = Room.builder()
                .id(20L).name("private").type(Room.RoomType.PRIVATE)
                .createdBy(owner).members(new ArrayList<>()).build();

        RoomMember ownerMember = RoomMember.builder()
                .room(privateRoom).user(owner).role(RoomMember.MemberRole.OWNER).build();

        when(roomRepository.findByIdWithCreator(20L)).thenReturn(Optional.of(privateRoom));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(roomMemberRepository.findByRoomAndUser(privateRoom, owner)).thenReturn(Optional.of(ownerMember));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(member));
        when(roomMemberRepository.existsByRoomAndUser(privateRoom, member)).thenReturn(false);

        roomService.inviteUser(20L, "alice", "bob");

        verify(roomMemberRepository).save(argThat(m ->
                m.getUser().equals(member) && m.getRole() == RoomMember.MemberRole.MEMBER));
    }

    @Test
    @DisplayName("inviteUser: throws when inviting to PUBLIC room")
    void inviteUser_throwsForPublicRoom() {
        when(roomRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(room)); // PUBLIC

        assertThatThrownBy(() -> roomService.inviteUser(10L, "alice", "bob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Public rooms do not require");
    }

    @Test
    @DisplayName("inviteUser: throws when MEMBER tries to invite")
    void inviteUser_throwsWhenMemberTriesToInvite() {
        Room privateRoom = Room.builder()
                .id(20L).name("private").type(Room.RoomType.PRIVATE)
                .createdBy(owner).members(new ArrayList<>()).build();

        RoomMember regularMember = RoomMember.builder()
                .room(privateRoom).user(member).role(RoomMember.MemberRole.MEMBER).build();

        when(roomRepository.findByIdWithCreator(20L)).thenReturn(Optional.of(privateRoom));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(member));
        when(roomMemberRepository.findByRoomAndUser(privateRoom, member)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> roomService.inviteUser(20L, "bob", "charlie"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only OWNER or ADMIN");
    }

    // ── createOrGetDm ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createOrGetDm: throws when user tries to DM themselves")
    void createOrGetDm_throwsWhenSameUser() {
        assertThatThrownBy(() -> roomService.createOrGetDm("alice", "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create a DM with yourself");
    }

    @Test
    @DisplayName("createOrGetDm: returns existing DM room if already exists")
    void createOrGetDm_returnsExistingDm() {
        Room dmRoom = Room.builder()
                .id(30L).name("alice:bob").type(Room.RoomType.DIRECT)
                .createdBy(owner).members(new ArrayList<>()).build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(member));
        when(roomRepository.findDirectMessageRoom(owner, member)).thenReturn(Optional.of(dmRoom));

        RoomResponse response = roomService.createOrGetDm("alice", "bob");

        assertThat(response.getName()).isEqualTo("alice:bob");
        assertThat(response.getType()).isEqualTo(Room.RoomType.DIRECT);
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrGetDm: creates new DM room with deterministic name")
    void createOrGetDm_createsNewDm() {
        Room dmRoom = Room.builder()
                .id(30L).name("alice:bob").type(Room.RoomType.DIRECT)
                .createdBy(owner).members(new ArrayList<>()).build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(member));
        when(roomRepository.findDirectMessageRoom(owner, member)).thenReturn(Optional.empty());
        when(roomRepository.save(any(Room.class))).thenReturn(dmRoom);
        when(roomRepository.findByIdWithCreator(30L)).thenReturn(Optional.of(dmRoom));

        RoomResponse response = roomService.createOrGetDm("alice", "bob");

        assertThat(response.getName()).isEqualTo("alice:bob");
        verify(roomRepository).save(any(Room.class));
        verify(roomMemberRepository, times(2)).save(any(RoomMember.class));
    }

    // ── getPublicRooms ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPublicRooms: returns only PUBLIC rooms")
    void getPublicRooms_returnsPublicOnly() {
        when(roomRepository.findByType(Room.RoomType.PUBLIC)).thenReturn(List.of(room));

        List<RoomResponse> result = roomService.getPublicRooms();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(Room.RoomType.PUBLIC);
    }
}
