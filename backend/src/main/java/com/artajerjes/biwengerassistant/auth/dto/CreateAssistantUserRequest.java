package com.artajerjes.biwengerassistant.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssistantUserRequest(

        @NotBlank @Size(max = 50) String username,

        @NotBlank @Size(min = 8, max = 100) String password,

        @NotNull Long managerId) {
}