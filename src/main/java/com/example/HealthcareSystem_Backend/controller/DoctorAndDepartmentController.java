package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.dto.OperationalDTOs.DoctorRegistrationRequest;
import com.example.HealthcareSystem_Backend.entity.Department;
import com.example.HealthcareSystem_Backend.entity.Doctor;
import com.example.HealthcareSystem_Backend.repository.DepartmentRepository;
import com.example.HealthcareSystem_Backend.repository.DoctorRepository;
import com.example.HealthcareSystem_Backend.service.HospitalServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DoctorAndDepartmentController {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final HospitalServices hospitalServices;

    // --- Doctor Endpoints ---

    // Viewable by all medical/administrative and lab staff
    @GetMapping("/doctors")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST', 'LAB_STAFF', 'PHARMACIST', 'ACCOUNTANT')")
    public ResponseEntity<List<Doctor>> getDoctors() {
        return ResponseEntity.ok(doctorRepository.findAll());
    }

    // Only Admin & Receptionist can add doctors
    @PostMapping("/doctors")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Doctor> createDoctor(@RequestBody DoctorRegistrationRequest req, Principal principal) {
        String adminUser = (principal != null) ? principal.getName() : "ADMIN";
        return ResponseEntity.ok(hospitalServices.createDoctor(req, adminUser));
    }

    // Only Admin can delete doctors
    @DeleteMapping("/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDoctor(@PathVariable Long id, Principal principal) {
        String adminUser = (principal != null) ? principal.getName() : "ADMIN";
        hospitalServices.deleteDoctor(id, adminUser);
        return ResponseEntity.ok("Doctor removed successfully");
    }

    // --- Department Endpoints ---

    // Viewable across the hospital
    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST', 'LAB_STAFF', 'PHARMACIST', 'ACCOUNTANT')")
    public ResponseEntity<List<Department>> getDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    // Admin-only creation
    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> createDepartment(@RequestBody Department dept) {
        return ResponseEntity.ok(departmentRepository.save(dept));
    }
}