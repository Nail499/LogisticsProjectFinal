package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tracking_logs")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TrackingLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id",nullable = false)
    Trip trip;

    @Column(nullable = false)
     Double latitude;

    @Column(nullable = false)
     Double longitude;

     LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        this.recordedAt = LocalDateTime.now();
    }
}
