package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Admin tərəfindən idarə olunan tarif cədvəli — CustomsDutyService bu
// cədvəldən Cargo.cargoType-a görə rüsum/ƏDV faizini axtarır. Real gömrük
// tarif kitabı minlərlə HS koddan ibarətdir; burada sadələşdirilib, mövcud
// dörd CargoType (GENERAL/FRAGILE/REFRIGERATED/HAZARDOUS) üzərindən
// hesablanır ki, tarif inzibatçılığı real və idarə edilə bilən qalsın.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "customs_tariffs")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomsTariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    CargoType cargoType;

    @Column(nullable = false)
    Double dutyRatePercent;

    @Column(nullable = false)
    Double vatRatePercent;

    String description;

    LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
