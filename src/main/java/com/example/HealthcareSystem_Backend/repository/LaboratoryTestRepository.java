package com.example.HealthcareSystem_Backend.repository;

import com.example.HealthcareSystem_Backend.entity.LaboratoryTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LaboratoryTestRepository extends JpaRepository<LaboratoryTest, Long> {
    List<LaboratoryTest> findByPatientId(Long patientId);

    List<LaboratoryTest> findByStatus(LaboratoryTest.LabStatus status);

    void deleteByPatientId(Long patientId);

    void deleteByStatus(LaboratoryTest.LabStatus status);

    List<LaboratoryTest> findByPatientIdIn(List<Long> patientIds);
}