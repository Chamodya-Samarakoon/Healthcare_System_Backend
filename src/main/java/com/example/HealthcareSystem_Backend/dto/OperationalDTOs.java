package com.example.HealthcareSystem_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OperationalDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PatientRequest {
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String gender;
        private String nic;
        private String phone;
        private String email;
        private String address;
        private String emergencyContact;
        private String bloodGroup;
        private String conditionSummary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicalRecordRequest {
        private Long patientId;
        private Long doctorId;
        private String symptoms;
        private String diagnosis;
        private String treatment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrescriptionItemRequest {
        private Long medicineId;
        private Integer quantity;
        private String dosage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrescriptionRequest {
        private Long patientId;
        private Long doctorId;
        private List<PrescriptionItemRequest> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceRequest {
        private Long patientId;
        private Double totalAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentRequest {
        private Long invoiceId;
        private Double amount;
        private String paymentMethod;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorRegistrationRequest {
        private String username;
        private String password;
        private String fullName;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String specialization;
        private Long departmentId;
        private String workingDays;
        private String workingHours;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppointmentRequest {
        private Long patientId;
        private Long doctorId;
        private LocalDateTime appointmentTime;
        private String reason;
        private String notes;
    }
}