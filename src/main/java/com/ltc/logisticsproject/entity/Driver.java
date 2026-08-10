package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "drivers")
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String fullName;

    @Column(nullable = false,unique = true)
    String phone;

     String licenseNumber;
     String licenseDocumentUrl;

    // Profile settings (self-service, see ProfileController)
     String photoUrl;
     LocalDate dateOfBirth;
     String nationality;
     String location;
     // Customer.email ilə eyni pattern — profil səhifəsindən kodla dəyişilə bilər.
     String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus status;

    private Long jobApplicationId;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = DriverStatus.ACTIVE;
    }
}
