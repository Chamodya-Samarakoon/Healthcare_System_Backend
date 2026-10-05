package com.example.HealthcareSystem_Backend.repository;

import com.example.HealthcareSystem_Backend.entity.StaffAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long> {
    List<StaffAttendance> findByAttendanceDate(LocalDate attendanceDate);

    Optional<StaffAttendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    List<StaffAttendance> findByEmployeeId(Long employeeId);
}