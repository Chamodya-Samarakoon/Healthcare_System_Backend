package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.entity.Medicine;
import com.example.HealthcareSystem_Backend.repository.MedicineRepository;
import com.example.HealthcareSystem_Backend.service.HospitalServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy/medicines")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PharmacyController {

    private final MedicineRepository medicineRepository;
    private final HospitalServices hospitalServices;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'PHARMACIST', 'ACCOUNTANT', 'RECEPTIONIST')")
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        return ResponseEntity.ok(medicineRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<Medicine> addMedicine(@RequestBody Medicine medicine, Authentication auth) {
        if (medicine.getReorderLevel() == null) {
            medicine.setReorderLevel(10);
        }
        Medicine saved = medicineRepository.save(medicine);

        if (auth != null) {
            hospitalServices.recordAudit(
                    auth.getName(),
                    "CREATE",
                    "PHARMACY",
                    "Added medicine to inventory: " + saved.getName());
        }
        return ResponseEntity.ok(saved);
    }
}