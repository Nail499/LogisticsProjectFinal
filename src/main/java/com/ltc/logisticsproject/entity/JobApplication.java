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

    // İctimai (auth-suz) status yoxlama endpoint-i əvvəllər ardıcıl DB id-si
    // ilə axtarırdı — bu həm "kod" kimi peşəkar görünmürdü, həm də təhlükəsizlik
    // problemi idi (kimsə 1,2,3... deyə sadalayaraq başqasının müraciət
    // statusunu/rədd səbəbini görə bilərdi). Cargo.trackingNumber ilə eyni
    // naxışla unikal, təxmin edilməsi çətin kod yaradılır.
    @Column(unique = true)
    String applicationCode;

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
        if (this.applicationCode == null) {
            this.applicationCode = "APP" + System.currentTimeMillis();
        }
    }

}
