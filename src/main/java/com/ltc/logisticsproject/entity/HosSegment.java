package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Server-tərəfli HOS (iş saatı) qeydiyyatı — əvvəlki client-side RestModeCard
// stopwatch-ı əvəz edir (o, tarayıcı bağlananda/yenilənəndə sıfırlanırdı).
// Reys üzrə DRIVING/RESTING seqmentləri ardıcıl saxlanılır; hər an ən çoxu
// BİR açıq (endedAt=null) seqment ola bilər (bax HosService — toggle əvvəlki
// açıq seqmenti bağlayıb yenisini açır). schema avtomatik yaradılır
// (ddl-auto=update).
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "hos_segments")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HosSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    HosStatus status;

    @Column(nullable = false)
    LocalDateTime startedAt;

    // null = hazırda davam edən (açıq) seqment.
    LocalDateTime endedAt;

    // DRIVING seqmenti 4.5 saatı keçəndə bir dəfə FatigueAlert yaradılır —
    // bu bayraq təkrar-təkrar xəbərdarlıq yaranmasının qarşısını alır
    // (bax HosService#buildStatus).
    @Builder.Default
    Boolean alertSent = false;
}
