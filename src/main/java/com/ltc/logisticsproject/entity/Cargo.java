package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cargos")
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false,unique = true)
    String trackingNumber;

    String description;
    Double weight;
    Double volume;

    @ManyToOne
    @JoinColumn(name = "origin_warehouse_id")
    Warehouse originWarehouse;

    @ManyToOne
    @JoinColumn(name = "customer_id")
     Customer customer;

    @Enumerated(EnumType.STRING)
     CargoType cargoType;

    @Enumerated(EnumType.STRING)
     UrgencyLevel urgency;

     java.time.LocalDate requestedPickupDate;

     String pickupAddress;
     Double pickupLatitude;
     Double pickupLongitude;

     String destinationAddress;
     Double destinationLatitude;
     Double destinationLongitude;

     String customerName;
     String customerPhone;

    // Beynəlxalq göndəriş sahələri — daxili (Azərbaycan daxili) sifarişlərdə
    // boş qalır, dispetçer/müştəri "Beynəlxalq göndərişdir" seçəndə doldurulur.
    @Enumerated(EnumType.STRING)
    TransportMode preferredTransportMode;

    @Enumerated(EnumType.STRING)
    Incoterm incoterm;

    String originCountry;
    String destinationCountry;
    // Sadələşdirilmiş saxlama — vergüllə ayrılmış ölkə adları (məs.
    // "Gürcüstan, Türkiyə"), ayrıca cədvəl qurmadan tranzit marşrutunu izah edir.
    String transitCountries;
    // Boolean (primitiv boolean yox) qəsdən seçilib: "boolean" DB sütununu
    // NOT NULL kimi yaradır, bu isə artıq sətirləri olan mövcud "cargos"
    // cədvəlinə ALTER TABLE zamanı "contains null values" xətası ilə
    // uğursuz olurdu. Boolean nullable sütun yaradır, köhnə sətirlər
    // avtomatik NULL alır — kodda Boolean.TRUE.equals(...) ilə oxunur.
    Boolean requiresCustoms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
     CargoStatus status;

    @ManyToOne
    @JoinColumn(name = "trip_id")
     Trip trip;

     LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = CargoStatus.PENDING;
        if (this.trackingNumber == null) {
            this.trackingNumber = "TRK" + System.currentTimeMillis();
        }
    }
}
