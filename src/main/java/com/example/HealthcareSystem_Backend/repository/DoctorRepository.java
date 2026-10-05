package com.example.HealthcareSystem_Backend.repository;

import com.example.HealthcareSystem_Backend.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}