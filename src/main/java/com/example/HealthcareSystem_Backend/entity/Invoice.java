package com.example.HealthcareSystem_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", unique = true)
    private String invoiceNumber;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    private Double totalAmount;
    private Double paidAmount;

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, PARTIALLY_PAID, PAID

    private LocalDateTime createdAt;

    public enum Status {
        PENDING, PARTIALLY_PAID, PAID
    }

    @PrePersist
    public void prePersist() {
        if (this.invoiceNumber == null || this.invoiceNumber.isBlank()) {
            this.invoiceNumber = "INV-" + System.currentTimeMillis();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.paidAmount == null) {
            this.paidAmount = 0.0;
        }
    }
}