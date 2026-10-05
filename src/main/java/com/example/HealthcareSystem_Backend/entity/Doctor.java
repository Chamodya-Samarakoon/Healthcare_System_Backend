package com.example.HealthcareSystem_Backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doctors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("first_name")
    @Column(name = "first_name")
    private String firstName;

    @JsonProperty("last_name")
    @Column(name = "last_name")
    private String lastName;

    @JsonProperty("full_name")
    @Column(name = "full_name")
    private String fullName;

    private String specialization;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    private String phone;
    private String email;

    @JsonProperty("workingDays")
    @Column(name = "working_days")
    private String workingDays;

    @JsonProperty("workingHours")
    @Column(name = "working_hours")
    private String workingHours;

    private String status;

    @Builder.Default
    private Boolean active = true;

    @PrePersist
    @PreUpdate
    private void setDefaults() {
        if (this.active == null) {
            this.active = true;
        }
        if (this.fullName == null && this.firstName != null) {
            this.fullName = (this.lastName != null)
                    ? this.firstName + " " + this.lastName
                    : this.firstName;
        }
    }
}