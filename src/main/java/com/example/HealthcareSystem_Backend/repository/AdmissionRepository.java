package com.example.HealthcareSystem_Backend.repository;

import com.example.HealthcareSystem_Backend.entity.Admission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdmissionRepository extends JpaRepository<Admission, Long> {
    void deleteByPatientId(Long patientId);

    // Check if the bed is already occupied by a non-discharged patient
    boolean existsByWardNumberIgnoreCaseAndBedNumberIgnoreCaseAndStatus(
            String wardNumber,
            String bedNumber,
            String status);

    // Check if the patient is already admitted in any ward
    boolean existsByPatientIdAndStatus(Long patientId, String status);
}