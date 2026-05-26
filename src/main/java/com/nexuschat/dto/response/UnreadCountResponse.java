package com.nexuschat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnreadCountResponse {
    private Long roomId;
    private long unreadCount;
}
