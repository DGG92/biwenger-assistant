package com.artajerjes.biwengerassistant.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank @Size(min = 8, max = 100) String newPassword,

        @NotBlank @Size(min = 8, max = 100) String repeatedPassword) {
}