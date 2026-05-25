package com.nexuschat.dto.response;

import com.nexuschat.model.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long id;
    private String name;
    private String description;
    private Room.RoomType type;
    private Long createdById;
    private String createdByUsername;
    private int memberCount;
    private LocalDateTime createdAt;

    public static RoomResponse from(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .type(room.getType())
                .createdById(room.getCreatedBy().getId())
                .createdByUsername(room.getCreatedBy().getUsername())
                .memberCount(room.getMembers() != null ? room.getMembers().size() : 0)
                .createdAt(room.getCreatedAt())
                .build();
    }
}
