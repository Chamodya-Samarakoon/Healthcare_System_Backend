package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.entity.LaboratoryTest;
import com.example.HealthcareSystem_Backend.entity.Patient;
import com.example.HealthcareSystem_Backend.exception.BadRequestException;
import com.example.HealthcareSystem_Backend.exception.ResourceNotFoundException;
import com.example.HealthcareSystem_Backend.repository.LaboratoryTestRepository;
import com.example.HealthcareSystem_Backend.repository.PatientRepository;
import com.example.HealthcareSystem_Backend.service.HospitalServices;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/laboratory/tests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LaboratoryController {

    private final LaboratoryTestRepository labTestRepository;
    private final PatientRepository patientRepository;
    private final HospitalServices hospitalServices;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'LAB_STAFF', 'ACCOUNTANT')")
    public ResponseEntity<List<LaboratoryTest>> getAllTests() {
        return ResponseEntity.ok(labTestRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'LAB_STAFF', 'NURSE')")
    public ResponseEntity<LaboratoryTest> requestTest(@RequestBody LabRequest request, Authentication auth) {
        Long resolvedPatientId = request.getResolvedPatientId();
        if (resolvedPatientId == null) {
            throw new BadRequestException("Patient ID cannot be null");
        }

        Patient patient = patientRepository.findById(resolvedPatientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + resolvedPatientId));

        LaboratoryTest test = LaboratoryTest.builder()
                .testName(request.getTestName())
                .patient(patient)
                .status(LaboratoryTest.LabStatus.REQUESTED)
                .requestedDate(LocalDateTime.now())
                .build();

        LaboratoryTest saved = labTestRepository.save(test);

        if (auth != null) {
            hospitalServices.recordAudit(
                    auth.getName(),
                    "CREATE",
                    "LABORATORY",
                    "Requested test '" + test.getTestName() + "' for patient #" + patient.getId());
        }

        return ResponseEntity.ok(saved);
    }

    @RequestMapping(value = { "/{id}", "/{id}/result" }, method = { RequestMethod.PUT, RequestMethod.PATCH })
    @PreAuthorize("hasAnyRole('ADMIN', 'LAB_STAFF')")
    public ResponseEntity<LaboratoryTest> updateTestResult(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        LaboratoryTest test = labTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab test not found with id: " + id));

        // Read result text from any common property name
        String resultVal = null;
        if (body.containsKey("results") && body.get("results") != null) {
            resultVal = body.get("results").toString();
        } else if (body.containsKey("resultDetails") && body.get("resultDetails") != null) {
            resultVal = body.get("resultDetails").toString();
        } else if (body.containsKey("result") && body.get("result") != null) {
            resultVal = body.get("result").toString();
        }

        if (resultVal != null) {
            test.setResultDetails(resultVal);
        }

        // Handle status if passed by the frontend, otherwise mark COMPLETED
        if (body.containsKey("status") && body.get("status") != null) {
            try {
                test.setStatus(LaboratoryTest.LabStatus.valueOf(body.get("status").toString().toUpperCase()));
            } catch (IllegalArgumentException e) {
                test.setStatus(LaboratoryTest.LabStatus.COMPLETED);
            }
        } else {
            test.setStatus(LaboratoryTest.LabStatus.COMPLETED);
        }

        test.setCompletedDate(LocalDateTime.now());

        LaboratoryTest updated = labTestRepository.save(test);

        if (auth != null) {
            hospitalServices.recordAudit(
                    auth.getName(),
                    "UPDATE",
                    "LABORATORY",
                    "Updated results for lab test #" + id);
        }

        return ResponseEntity.ok(updated);
    }

    @Data
    public static class LabRequest {
        @JsonAlias({ "patient_id", "patientId" })
        private Long patientId;

        // Handles cases where frontend sends { patient: { id: 1 } }
        private Map<String, Object> patient;

        private String testName;

        public Long getResolvedPatientId() {
            if (this.patientId != null) {
                return this.patientId;
            }
            if (this.patient != null && this.patient.get("id") != null) {
                Object idObj = this.patient.get("id");
                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();
                }
                return Long.parseLong(idObj.toString());
            }
            return null;
        }
    }
}