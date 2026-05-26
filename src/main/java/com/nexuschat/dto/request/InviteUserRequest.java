package com.nexuschat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteUserRequest {

    @NotBlank(message = "Username is required")
    private String username;
}
