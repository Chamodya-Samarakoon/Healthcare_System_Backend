package com.example.HealthcareSystem_Backend.dto;

import com.example.HealthcareSystem_Backend.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTOs {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class LoginResponse {
        private String token;
        private Long userId;
        private String username;
        private Role role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserRequest {
        private String username;
        private String password;
        private String fullName;
        private String email;
        private Role role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResetPasswordRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "New password is required")
        private String newPassword;
    }
}