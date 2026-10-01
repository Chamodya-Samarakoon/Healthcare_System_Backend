package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.dto.AuthDTOs.UserRequest;
import com.example.HealthcareSystem_Backend.entity.User;
import com.example.HealthcareSystem_Backend.repository.UserRepository;
import com.example.HealthcareSystem_Backend.service.HospitalServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final HospitalServices hospitalServices;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserRequest request, Authentication auth) {
        return ResponseEntity.ok(hospitalServices.createUser(request, auth.getName()));
    }
}