package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Dispetçer üçün "Qoşqu hovuzu" ekranı (bax DispatcherController#trailerPool,
// frontend/TrailerPool.jsx) — real TMS platformalarında (məs. McLeod) bu,
// "drop-and-hook" idarəetməsinin nüvəsidir: hansı qoşqu haradadır, boşdur
// yoxsa yüklüdür, hansı reysə bağlıdır. Əvvəllər dispetçer bunu heç bir yerdə
// görə bilmirdi — qoşqu yalnız "Reys yarat" formasındakı seçim siyahısında
// görünürdü, onun HAZIRKI vəziyyəti (harada, boşdurmu) heç yerdə izlənmirdi.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TrailerPoolResponse {
    Long id;
    String plateNumber;
    Double capacity;
    String ownerType; // "COMPANY" | "DRIVER_OWNED"
    String ownerDriverName; // yalnız DRIVER_OWNED olduqda dolur

    // false-dursa qoşqu heç bir aktiv reysə bağlı deyil — bazadadır/boşdadır.
    boolean onTrip;
    Long activeTripId;
    String tripStatus; // PLANNED | PICKED_UP | IN_TRANSIT
    // Trip.status-a görə sadə nəticə: PICKED_UP/IN_TRANSIT = yüklüdür,
    // PLANNED = hələ boşdur (yükə doğru gedir).
    boolean loaded;
    String driverName;
    String vehiclePlate;

    Double lastLatitude;
    Double lastLongitude;
    String lastUpdatedAt;
}
