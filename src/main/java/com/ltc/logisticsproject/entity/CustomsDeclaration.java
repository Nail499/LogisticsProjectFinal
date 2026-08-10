package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Bir beynəlxalq Cargo üçün gömrük bəyannaməsi. Rüsum/ƏDV məbləği
// CustomsDutyService tərəfindən CustomsTariff cədvəlinə əsasən hesablanır
// (bax service paketi) — real dövlət gömrük sistemi ilə inteqrasiya deyil,
// sistemin öz daxili hesablama məntiqidir.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "customs_declarations")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomsDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "cargo_id", nullable = false, unique = true)
    Cargo cargo;

    @Column(unique = true)
    String declarationNumber;

    String originCountry;
    String destinationCountry;

    // Harmonized System kodu — malın beynəlxalq gömrük təsnifat kodu
    // (tarif CustomsTariff-da bu kod əsasında deyil, sadələşdirmə üçün
    // Cargo.cargoType əsasında axtarılır, amma HS kodu sənəddə saxlanılır).
    String hsCode;

    Double declaredValue;
    String currency;

    Double dutyRatePercent;
    Double vatRatePercent;
    Double dutyAmount;
    Double vatAmount;
    Double totalPayable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DeclarationStatus status;

    String notes;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime submittedAt;
    LocalDateTime clearedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) this.status = DeclarationStatus.DRAFT;
        if (this.currency == null) this.currency = "AZN";
        if (this.declarationNumber == null) {
            this.declarationNumber = "CD" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
