package com.example.HealthcareSystem_Backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportDTOs {

    @Data
    @Builder
    public static class PatientReportDTO {
        private long totalRegistered;
        private long totalAdmittedNow;
        private Map<String, Long> genderDistribution;
        private Map<String, Long> bloodGroupDistribution;
        private List<PatientSummary> recentRegistrations;
    }

    @Data
    @Builder
    public static class PatientSummary {
        private String patientNumber;
        private String fullName;
        private String gender;
        private String bloodGroup;
        private LocalDate registrationDate;
    }

    @Data
    @Builder
    public static class AppointmentReportDTO {
        private long totalAppointments;
        private long scheduledCount;
        private long completedCount;
        private long cancelledCount;
        private List<DoctorAppointmentLoad> doctorLoads;
    }

    @Data
    @Builder
    public static class DoctorAppointmentLoad {
        private String doctorName;
        private String specialization;
        private long appointmentCount;
    }

    @Data
    @Builder
    public static class RevenueReportDTO {
        private double totalBilled;
        private double totalCollected;
        private double outstandingBalance;
        private long paidInvoicesCount;
        private long pendingInvoicesCount;
        private Map<String, Double> paymentMethodBreakdown;
    }

    @Data
    @Builder
    public static class PharmacyReportDTO {
        private long totalMedicines;
        private long lowStockCount;
        private long expiredCount;
        private long totalDispensedPrescriptions;
        private List<LowStockAlert> lowStockAlerts;
    }

    @Data
    @Builder
    public static class LowStockAlert {
        private String medicineName;
        private int currentStock;
        private LocalDate expiryDate;
    }

    @Data
    @Builder
    public static class LaboratoryReportDTO {
        private long totalTestsConducted;
        private long pendingResultsCount;
        private long completedResultsCount;
        private Map<String, Long> topTestsConducted;
    }

    @Data
    @Builder
    public static class StaffReportDTO {
        private long totalEmployees;
        private long activeCount;
        private long onLeaveCount;
        private long presentTodayCount;
        private Map<String, Long> departmentHeadcounts;
    }
}