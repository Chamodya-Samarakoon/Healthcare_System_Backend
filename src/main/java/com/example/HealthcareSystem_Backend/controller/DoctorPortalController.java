package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.entity.*;
import com.example.HealthcareSystem_Backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorPortalController {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final LaboratoryTestRepository labRepository;
    private final UserRepository userRepository;

    // Helper to get Doctor entity from the JWT authentication principal
    private Doctor getLoggedInDoctor(Authentication auth) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("No Doctor profile associated with account: " + username));
    }

    // 1. Get Logged-in Doctor's Profile
    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Doctor> getCurrentDoctor(Authentication auth) {
        return ResponseEntity.ok(getLoggedInDoctor(auth));
    }

    // 2. Get Appointments assigned only to this Doctor
    @GetMapping("/appointments")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<Appointment>> getDoctorAppointments(Authentication auth) {
        Doctor doctor = getLoggedInDoctor(auth);
        return ResponseEntity.ok(appointmentRepository.findByDoctorId(doctor.getId()));
    }

    // 3. Get distinct Patients assigned to this Doctor through appointments
    @GetMapping("/patients")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<Patient>> getDoctorPatients(Authentication auth) {
        Doctor doctor = getLoggedInDoctor(auth);
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctor.getId());

        List<Patient> uniquePatients = appointments.stream()
                .map(Appointment::getPatient)
                .distinct()
                .collect(Collectors.toList());

        return ResponseEntity.ok(uniquePatients);
    }

    // 4. Get Laboratory Results for this Doctor's patients
    @GetMapping("/lab-results")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<LaboratoryTest>> getDoctorPatientLabResults(Authentication auth) {
        Doctor doctor = getLoggedInDoctor(auth);
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctor.getId());

        List<Long> patientIds = appointments.stream()
                .map(a -> a.getPatient().getId())
                .distinct()
                .collect(Collectors.toList());

        return ResponseEntity.ok(labRepository.findByPatientIdIn(patientIds));
    }
}