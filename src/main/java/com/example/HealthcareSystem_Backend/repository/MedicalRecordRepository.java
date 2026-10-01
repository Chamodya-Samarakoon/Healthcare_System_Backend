package com.example.HealthcareSystem_Backend.repository;

import com.example.HealthcareSystem_Backend.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientId(Long patientId);

    void deleteByPatientId(Long patientId);

    void deleteByDoctorId(Long doctorId);
}