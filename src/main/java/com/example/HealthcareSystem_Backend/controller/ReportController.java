package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.dto.ReportDTOs.*;
import com.example.HealthcareSystem_Backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/patients")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<PatientReportDTO> getPatientReport() {
        return ResponseEntity.ok(reportService.getPatientReport());
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<AppointmentReportDTO> getAppointmentReport() {
        return ResponseEntity.ok(reportService.getAppointmentReport());
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<RevenueReportDTO> getRevenueReport() {
        return ResponseEntity.ok(reportService.getRevenueReport());
    }

    @GetMapping("/pharmacy")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'DOCTOR')")
    public ResponseEntity<PharmacyReportDTO> getPharmacyReport() {
        return ResponseEntity.ok(reportService.getPharmacyReport());
    }

    @GetMapping("/laboratory")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAB_STAFF', 'DOCTOR')")
    public ResponseEntity<LaboratoryReportDTO> getLaboratoryReport() {
        return ResponseEntity.ok(reportService.getLaboratoryReport());
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffReportDTO> getStaffReport() {
        return ResponseEntity.ok(reportService.getStaffReport());
    }
}