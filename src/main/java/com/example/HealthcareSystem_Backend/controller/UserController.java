package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.dto.AuthDTOs.UserRequest;
import com.example.HealthcareSystem_Backend.entity.User;
import com.example.HealthcareSystem_Backend.repository.UserRepository;
import com.example.HealthcareSystem_Backend.service.HospitalServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final HospitalServices hospitalServices;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRequest req, Authentication auth) {
        String admin = (auth != null) ? auth.getName() : "ADMIN";
        return ResponseEntity.ok(hospitalServices.createUser(req, admin));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        String admin = (auth != null) ? auth.getName() : "ADMIN";
        hospitalServices.deleteUser(id, admin);
        return ResponseEntity.noContent().build();
    }
}