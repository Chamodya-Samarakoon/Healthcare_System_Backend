package com.example.HealthcareSystem_Backend.repository;

import com.example.HealthcareSystem_Backend.entity.LeaveRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRecordRepository extends JpaRepository<LeaveRecord, Long> {
    List<LeaveRecord> findByEmployeeId(Long employeeId);

    List<LeaveRecord> findByStatus(LeaveRecord.LeaveStatus status);
}