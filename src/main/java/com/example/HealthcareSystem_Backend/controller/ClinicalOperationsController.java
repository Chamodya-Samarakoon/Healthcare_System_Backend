package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.dto.OperationalDTOs.MedicalRecordRequest;
import com.example.HealthcareSystem_Backend.dto.OperationalDTOs.PrescriptionRequest;
import com.example.HealthcareSystem_Backend.entity.*;
import com.example.HealthcareSystem_Backend.repository.*;
import com.example.HealthcareSystem_Backend.service.HospitalServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClinicalOperationsController {

    private final MedicalRecordRepository medicalRecordRepository;
    private final LaboratoryTestRepository labRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final HospitalServices hospitalServices;

    // --- Medical Records ---
    @GetMapping("/medical-records/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST', 'LAB_STAFF', 'PHARMACIST')")
    public ResponseEntity<List<MedicalRecord>> getPatientRecords(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicalRecordRepository.findByPatientId(patientId));
    }

    @PostMapping("/medical-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<MedicalRecord> addRecord(@RequestBody MedicalRecordRequest req, Authentication auth) {
        String username = (auth != null) ? auth.getName() : "ADMIN";
        return ResponseEntity.ok(hospitalServices.addMedicalRecord(req, username));
    }

    // --- Laboratory ---
    @GetMapping("/laboratory/tests")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'LAB_STAFF', 'NURSE', 'ACCOUNTANT')")
    public ResponseEntity<List<LaboratoryTest>> getLabTests() {
        return ResponseEntity.ok(labRepository.findAll());
    }

    @PostMapping("/laboratory/tests")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<LaboratoryTest> requestLabTest(@RequestBody LaboratoryTest test) {
        test.setStatus(LaboratoryTest.LabStatus.REQUESTED);
        return ResponseEntity.ok(labRepository.save(test));
    }

    // --- Pharmacy ---
    @GetMapping("/pharmacy/medicines")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'DOCTOR', 'NURSE', 'ACCOUNTANT')")
    public ResponseEntity<List<Medicine>> getMedicines() {
        return ResponseEntity.ok(medicineRepository.findAll());
    }

    @PostMapping("/pharmacy/medicines")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<Medicine> saveMedicine(@RequestBody Medicine medicine) {
        return ResponseEntity.ok(medicineRepository.save(medicine));
    }

    @PostMapping("/pharmacy/prescriptions")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<Prescription> createPrescription(@RequestBody PrescriptionRequest req, Authentication auth) {
        String username = (auth != null) ? auth.getName() : "ADMIN";
        return ResponseEntity.ok(hospitalServices.createPrescription(req, username));
    }

    @PostMapping("/pharmacy/prescriptions/{id}/dispense")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<String> dispense(@PathVariable Long id, Authentication auth) {
        String username = (auth != null) ? auth.getName() : "ADMIN";
        hospitalServices.dispensePrescription(id, username);
        return ResponseEntity.ok("Prescription dispensed and inventory reduced successfully.");
    }
}