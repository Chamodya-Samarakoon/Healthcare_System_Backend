package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.dto.OperationalDTOs.AppointmentRequest;
import com.example.HealthcareSystem_Backend.entity.Appointment;
import com.example.HealthcareSystem_Backend.exception.BadRequestException;
import com.example.HealthcareSystem_Backend.exception.ResourceNotFoundException;
import com.example.HealthcareSystem_Backend.repository.AppointmentRepository;
import com.example.HealthcareSystem_Backend.service.HospitalServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final HospitalServices hospitalServices;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Appointment> createAppointment(@RequestBody AppointmentRequest request, Authentication auth) {
        return ResponseEntity.ok(hospitalServices.bookAppointment(request, auth.getName()));
    }

    // Endpoint to update appointment status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<Appointment> updateAppointmentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        String newStatusStr = body.get("status");
        if (newStatusStr == null) {
            throw new BadRequestException("Status is required");
        }

        Appointment.Status newStatus = Appointment.Status.valueOf(newStatusStr.toUpperCase());
        appointment.setStatus(newStatus);
        Appointment updated = appointmentRepository.save(appointment);

        hospitalServices.recordAudit(
                auth != null ? auth.getName() : "ADMIN",
                "UPDATE",
                "APPOINTMENT",
                "Updated appointment #" + id + " status to " + newStatus);

        return ResponseEntity.ok(updated);
    }

    // Fixed mapping: now resolves to PUT /api/appointments/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Appointment> updateAppointment(
            @PathVariable Long id,
            @RequestBody AppointmentRequest req,
            Principal principal) {
        String adminUser = (principal != null) ? principal.getName() : "ADMIN";
        return ResponseEntity.ok(hospitalServices.updateAppointment(id, req, adminUser));
    }
}