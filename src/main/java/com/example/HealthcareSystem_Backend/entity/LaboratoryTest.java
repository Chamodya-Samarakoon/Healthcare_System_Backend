package com.example.HealthcareSystem_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "laboratory_tests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaboratoryTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    private String testName;

    @Enumerated(EnumType.STRING)
    private LabStatus status; // REQUESTED, SAMPLE_COLLECTED, COMPLETED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String resultDetails;

    private LocalDateTime requestedDate;
    private LocalDateTime completedDate;

    public enum LabStatus {
        REQUESTED, SAMPLE_COLLECTED, COMPLETED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        if (this.requestedDate == null) {
            this.requestedDate = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = LabStatus.REQUESTED;
        }
    }
}