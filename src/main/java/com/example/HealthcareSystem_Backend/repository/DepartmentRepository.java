package com.example.HealthcareSystem_Backend.repository;

import com.example.HealthcareSystem_Backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}