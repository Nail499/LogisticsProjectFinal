package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Beynəlxalq göndərişlərdə tələb olunan ticarət sənədləri (invoys, packing
// list, mənşə sertifikatı, CMR/konosament, tranzit sənədi) — hər Cargo bir
// neçə sənəd daşıya bilər. Fayl mövcud FileStorageService vasitəsilə
// yerli diskə yazılır (Trip.proofOfDeliveryUrl-da olduğu kimi eyni mexanizm).
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "trade_documents")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TradeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "cargo_id", nullable = false)
    Cargo cargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DocumentType type;

    @Column(nullable = false)
    String fileUrl;

    String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DocumentStatus status;

    // Sənədi kim yüklədi — Dispetçer/Admin adı, sadə mətn kimi saxlanılır
    // (ayrıca audit cədvəli qurmadan kifayət qədər izlənə bilən).
    String uploadedByName;

    String notes;

    LocalDateTime createdAt;
    LocalDateTime verifiedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = DocumentStatus.PENDING;
    }
}
