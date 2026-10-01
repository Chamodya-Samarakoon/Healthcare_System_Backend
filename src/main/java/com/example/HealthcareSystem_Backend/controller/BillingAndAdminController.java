package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.dto.OperationalDTOs.InvoiceRequest;
import com.example.HealthcareSystem_Backend.dto.OperationalDTOs.PaymentRequest;
import com.example.HealthcareSystem_Backend.entity.*;
import com.example.HealthcareSystem_Backend.exception.ResourceNotFoundException;
import com.example.HealthcareSystem_Backend.repository.*;
import com.example.HealthcareSystem_Backend.service.HospitalServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BillingAndAdminController {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final AdmissionRepository admissionRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;
    private final PatientRepository patientRepository;
    private final HospitalServices hospitalServices;

    // Billing
    @PostMapping("/billing/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Invoice> createInvoice(@RequestBody InvoiceRequest req) {
        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        Invoice invoice = Invoice.builder()
                .patient(patient)
                .totalAmount(req.getTotalAmount())
                .paidAmount(0.0)
                .status(Invoice.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @PostMapping("/billing/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Payment> makePayment(@RequestBody PaymentRequest req, Authentication auth) {
        return ResponseEntity.ok(hospitalServices.processPayment(req, auth.getName()));
    }

    // Get all Invoices with patient details
    @GetMapping("/billing/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'RECEPTIONIST')")
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        return ResponseEntity.ok(invoiceRepository.findAll());
    }

    // Get all Payments with invoice & patient details
    @GetMapping("/billing/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentRepository.findAll());
    }

    // Inpatient / Admissions
    @PostMapping("/admissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Admission> admitPatient(@RequestBody Admission admission) {
        admission.setAdmissionDate(LocalDateTime.now());
        admission.setStatus("ADMITTED");
        return ResponseEntity.ok(admissionRepository.save(admission));
    }

    // Get all Admissions
    @GetMapping("/admissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<List<Admission>> getAllAdmissions() {
        return ResponseEntity.ok(admissionRepository.findAll());
    }

    // Discharge patient
    @PutMapping("/admissions/{id}/discharge")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<Admission> dischargePatient(@PathVariable Long id) {
        Admission admission = admissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admission record not found"));
        admission.setStatus("DISCHARGED");
        admission.setDischargeDate(LocalDateTime.now());
        return ResponseEntity.ok(admissionRepository.save(admission));
    }

    // Staff
    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Employee>> getStaff() {
        return ResponseEntity.ok(employeeRepository.findAll());
    }

    // Audit Logs
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    // Analytics Dashboard
    @GetMapping("/reports/dashboard-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPatients", patientRepository.count());
        stats.put("totalInvoices", invoiceRepository.count());
        stats.put("totalPayments", paymentRepository.count());
        stats.put("admittedPatients",
                admissionRepository.findAll().stream().filter(a -> "ADMITTED".equals(a.getStatus())).count());
        return ResponseEntity.ok(stats);
    }
}