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

    // Müraciətçi öz tırı ilə işləmək istəyirsə true — bu halda aşağıdakı
    // vehicle* sahələri doldurulmalıdır (bax JobApplicationController#submit
    // validasiyası). false/null olduqda müraciətçinin tırı yoxdur, təsdiq
    // ediləndə (bax AdminApplicationService#approve) heç bir Vehicle
    // yaradılmır — sürücü sonra dispetçer tərəfindən reys-be-reys şirkət
    // tırı ilə təhkim olunur. NƏZƏRƏ AL: burada tutum (capacity) sahəsi
    // YOXDUR — tır yük daşımır, yalnız kəllə hissəsidir (bax Vehicle.java-
    // dakı ətraflı izah); əvvəllər olan vehicleCapacity sahəsi bu səbəbdən
    // silindi.
    Boolean hasOwnVehicle;
    String vehiclePlateNumber;
    String vehicleBrand;
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
