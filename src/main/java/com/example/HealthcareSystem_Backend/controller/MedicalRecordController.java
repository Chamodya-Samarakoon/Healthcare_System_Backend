package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.entity.Doctor;
import com.example.HealthcareSystem_Backend.entity.MedicalRecord;
import com.example.HealthcareSystem_Backend.entity.Patient;
import com.example.HealthcareSystem_Backend.exception.BadRequestException;
import com.example.HealthcareSystem_Backend.exception.ResourceNotFoundException;
import com.example.HealthcareSystem_Backend.repository.DoctorRepository;
import com.example.HealthcareSystem_Backend.repository.MedicalRecordRepository;
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
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MedicalRecordController {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalServices hospitalServices;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<List<MedicalRecord>> getAllRecords() {
        return ResponseEntity.ok(medicalRecordRepository.findAll());
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<List<MedicalRecord>> getRecordsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicalRecordRepository.findByPatientId(patientId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<MedicalRecord> createRecord(@RequestBody MedicalRecordRequest request, Authentication auth) {
        Long resolvedPatientId = request.getResolvedPatientId();
        if (resolvedPatientId == null) {
            throw new BadRequestException("Patient ID cannot be null");
        }

        Patient patient = patientRepository.findById(resolvedPatientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + resolvedPatientId));

        Doctor doctor = null;
        Long resolvedDoctorId = request.getResolvedDoctorId();
        if (resolvedDoctorId != null) {
            doctor = doctorRepository.findById(resolvedDoctorId).orElse(null);
        }

        MedicalRecord record = MedicalRecord.builder()
                .patient(patient)
                .doctor(doctor)
                .symptoms(request.getSymptoms())
                .diagnosis(request.getDiagnosis())
                .treatment(request.getTreatment())
                .visitDate(LocalDateTime.now())
                .build();

        MedicalRecord saved = medicalRecordRepository.save(record);

        if (auth != null) {
            hospitalServices.recordAudit(
                    auth.getName(),
                    "CREATE",
                    "MEDICAL_RECORDS",
                    "Added consultation record for patient #" + patient.getId());
        }

        return ResponseEntity.ok(saved);
    }

    @Data
    public static class MedicalRecordRequest {
        @JsonAlias({ "patient_id", "patientId" })
        private Long patientId;
        private Map<String, Object> patient;

        @JsonAlias({ "doctor_id", "doctorId" })
        private Long doctorId;
        private Map<String, Object> doctor;

        private String symptoms;
        private String diagnosis;
        private String treatment;

        public Long getResolvedPatientId() {
            if (this.patientId != null) {
                return this.patientId;
            }
            if (this.patient != null && this.patient.get("id") != null) {
                Object idObj = this.patient.get("id");
                return idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            }
            return null;
        }

        public Long getResolvedDoctorId() {
            if (this.doctorId != null) {
                return this.doctorId;
            }
            if (this.doctor != null && this.doctor.get("id") != null) {
                Object idObj = this.doctor.get("id");
                return idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
            }
            return null;
        }
    }
}