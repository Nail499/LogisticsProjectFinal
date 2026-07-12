package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "job_applications")
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String fullName;

    @Column(nullable = false)
    String phone;

    String licenseDocumentUrl;
    String vehiclePlateNumber;
    String vehicleBrand;
    Double vehicleCapacity;
    String vehicleDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ApplicationStatus status;

     LocalDateTime appliedAt;
     LocalDateTime reviewedAt;
     Long reviewedByAdminId;
     String rejectionReason;

    @PrePersist
    public void prePersist() {
        this.appliedAt = LocalDateTime.now();
        this.status = ApplicationStatus.PENDING;
    }

}
