package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Dispetçer üçün əsas KPI kartları (bax AdminReportService#getDispatcherKpis,
// KpiCards.jsx) — real TMS platformalarında dispetçerin gündəlik qərar
// verdiyi standart göstəricilər. NƏZƏRƏ AL: bu layihədə müştəriyə "vəd
// edilmiş çatdırılma tarixi" sahəsi YOXDUR (Cargo entity-də deadline sahəsi
// yoxdur) və real GPS-lə ölçülmüş "boş gedilən" məsafə də ayrıca izlənmir —
// ona görə onTimePercent və deadheadPercent AŞAĞIDA İZAH OLUNDUĞU KİMİ
// sadələşdirilmiş TƏXMİNLƏRDİR (RouteEstimationService/TripCostEstimation-
// Service-dəki eyni "documented estimate" fəlsəfəsi ilə), real ölçmə deyil.
// UI-da da bu aydın işarələnir (bax KpiCards.jsx info tooltip-ləri).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatcherKpiResponse {
    int deliveredTripsCount;

    // Çatdırılmış reyslərin neçə faizi (estimatedDistanceKm-dən hesablanan)
    // gözlənilən müddətə (+ 25% tolerans) sığıb — null, əgər heç bir DELIVERED
    // reysdə lazımi data (startedAt+deliveredAt+estimatedDistanceKm) yoxdur.
    Double onTimePercent;

    // Ardıcıl reyslər arasında (eyni sürücü üçün) əvvəlki təhvil nöqtəsi ilə
    // növbəti götürülmə nöqtəsi arasındakı təxmini "boş" məsafənin ümumi
    // gedilən məsafəyə nisbəti — null, əgər müqayisə üçün kifayət qədər
    // ardıcıl reys yoxdur.
    Double deadheadPercent;

    // Hazırkı anlıq görüntü (snapshot): qoşquların neçə faizi HAZIRDA
    // hansısa aktiv reysə bağlıdır (bax TrailerPoolResponse).
    double trailerUtilizationPercent;

    // Hazırda yüklü olan reyslərdə orta çəki-tutum nisbəti (yüklənmiş
    // çəki / bağlı qoşqunun tutumu) — boş qoşqular hesaba qatılmır.
    Double avgCapacityUtilizationPercent;
}
