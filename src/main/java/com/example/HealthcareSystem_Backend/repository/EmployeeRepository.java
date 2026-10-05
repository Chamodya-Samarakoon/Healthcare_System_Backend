package com.example.HealthcareSystem_Backend.repository;

import com.example.HealthcareSystem_Backend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDepartmentId(Long departmentId);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);
}