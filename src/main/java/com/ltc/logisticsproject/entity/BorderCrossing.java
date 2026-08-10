package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Bir reysin tranzit zamanı keçdiyi sərhəd/gömrük məntəqələrinin jurnalı.
// Ayrıca cədvəl olaraq saxlanılır (Trip.status maşınına toxunmadan) ki,
// çoxölkəli tranzitdə bir reys bir neçə sərhəd keçidi qeyd edə bilsin, və
// mövcud TripStatus (PLANNED/PICKED_UP/IN_TRANSIT/DELIVERED) axını
// pozulmasın.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "border_crossings")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BorderCrossing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    Trip trip;

    @Column(nullable = false)
    String borderPointName;

    String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    BorderCustomsStatus customsStatus;

    String recordedBy;
    String notes;

    LocalDateTime crossedAt;

    @PrePersist
    public void prePersist() {
        this.crossedAt = LocalDateTime.now();
        if (this.customsStatus == null) this.customsStatus = BorderCustomsStatus.PENDING;
    }
}
