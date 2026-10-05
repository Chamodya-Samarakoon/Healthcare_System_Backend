package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.dto.AuthDTOs.*;
import com.example.HealthcareSystem_Backend.service.HospitalServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final HospitalServices hospitalServices;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(hospitalServices.authenticate(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        hospitalServices.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
    }
}