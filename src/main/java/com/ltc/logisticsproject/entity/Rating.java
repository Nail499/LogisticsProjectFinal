package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Müştərinin çatdırılmış reysdən sonra sürücüyə verdiyi 1-5 ulduzlu
// qiymətləndirmə + sərbəst şərh. Bir reysdə bir neçə yük (müştəri)
// birləşdirilə bildiyi üçün (bax CargoQueue) açar tripId+customerId
// cütlüyüdür — hər müştəri eyni reysi yalnız BİR dəfə qiymətləndirə bilər,
// amma eyni reysdəki fərqli müştərilər ayrı-ayrı qiymət verə bilər.
// driverId reys yaradılanda sabitlənmiş sürücüyə görə denormallaşdırılıb
// (User/Customer/Notification-dakı eyni "plain Long id" konvensiyası) ki,
// ortalama hesablamaq üçün Trip cədvəlinə join lazım olmasın.
@Entity
@Table(name = "ratings", uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "customer_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "trip_id", nullable = false)
    Long tripId;

    @Column(name = "customer_id", nullable = false)
    Long customerId;

    @Column(nullable = false)
    Long driverId;

    @Column(nullable = false)
    Integer stars;

    @Column(length = 1000)
    String comment;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.stars != null) {
            this.stars = Math.max(1, Math.min(5, this.stars));
        }
    }
}
