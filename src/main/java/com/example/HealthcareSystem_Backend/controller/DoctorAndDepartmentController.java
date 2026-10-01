package com.example.HealthcareSystem_Backend.controller;

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
public class DoctorAndDepartmentController {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final HospitalServices hospitalServices;

    // --- Doctor Endpoints ---

    @GetMapping("/doctors")
    public ResponseEntity<List<Doctor>> getDoctors() {
        return ResponseEntity.ok(doctorRepository.findAll());
    }

    @PostMapping("/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Doctor> createDoctor(@RequestBody Doctor doctor) {
        return ResponseEntity.ok(doctorRepository.save(doctor));
    }

    @DeleteMapping("/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDoctor(@PathVariable Long id, Principal principal) {
        String adminUser = (principal != null) ? principal.getName() : "ADMIN";
        hospitalServices.deleteDoctor(id, adminUser);
        return ResponseEntity.ok("Doctor removed successfully");
    }

    // --- Department Endpoints ---

    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> createDepartment(@RequestBody Department dept) {
        return ResponseEntity.ok(departmentRepository.save(dept));
    }
}