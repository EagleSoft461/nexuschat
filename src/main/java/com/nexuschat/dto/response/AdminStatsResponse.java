package com.nexuschat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long totalRooms;
    private long totalMessages;
    private long activeUsers; // users online in last 24h
    private long publicRooms;
    private long privateRooms;
    private long directMessages;
}
