package com.example.HealthcareSystem_Backend.dto;

import com.example.HealthcareSystem_Backend.entity.Appointment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OperationalDTOs {

    @Data
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
    public static class AppointmentRequest {
        private Long patientId;
        private Long doctorId;
        private LocalDateTime appointmentTime;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicalRecordRequest {
        private Long patientId;
        private Long doctorId;
        private String symptoms;
        private String diagnosis;
        private String treatment;
    }

    @Data
    public static class PrescriptionItemRequest {
        private Long medicineId;
        private Integer quantity;
        private String dosage;
    }

    @Data
    public static class PrescriptionRequest {
        private Long patientId;
        private Long doctorId;
        private List<PrescriptionItemRequest> items;
    }

    @Data
    public static class InvoiceRequest {
        private Long patientId;
        private Double totalAmount;
    }

    @Data
    public static class PaymentRequest {
        private Long invoiceId;
        private Double amount;
        private String paymentMethod;
    }

}