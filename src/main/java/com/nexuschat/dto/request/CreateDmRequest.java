package com.nexuschat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDmRequest {

    @NotBlank(message = "Target username is required")
    private String targetUsername;
}
