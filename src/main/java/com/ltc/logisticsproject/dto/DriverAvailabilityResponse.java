package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Dispetçerin "Reys yarat" formasındakı sürücü seçimi üçün zənginləşdirilmiş
// sürücü kartı (bax DispatcherController#availableDrivers,
// CargoQueue.jsx driver select). Əvvəllər bu endpoint xam Driver entity
// qaytarırdı — dispetçer sürücünü seçərkən onun HOS (iş saatı) vəziyyətini
// və ya həll olunmamış DVIR defekti olub-olmadığını heç görmürdü, yalnız
// ad+telefon görürdü. Bu, real TMS platformalarında (Motive, Samsara)
// dispatch axınının nüvəsidir — uyğun olmayan sürücüyə YANLIŞLIQLA reys
// göndərilməsinin qarşısını UI səviyyəsində alır (backend enforcement YOXDUR,
// sırf görünürlük+xəbərdarlıq, CapacityCheckModal-dakı kimi dispetçer yenə
// də əl ilə davam edə bilər).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverAvailabilityResponse {
    Long id;
    String fullName;
    String phone;

    // Sürücünün HAZIRDA (PLANNED/PICKED_UP/IN_TRANSIT) reysi varmı — bax
    // HosService#getDriverSnapshot. false-dursa aşağıdakı HOS sahələri
    // "sıfırdan başlayır" mənasını daşıyır (yeni reysə tam saatla başlaya bilər).
    boolean hasActiveTrip;
    // "DRIVING" | "RESTING" | "NONE" — yalnız hasActiveTrip=true olduqda mənalıdır.
    String hosStatus;
    double todayDrivingHours;
    // HosService.MAX_DAILY_DRIVING_HOURS-a qədər qalan saat (sadələşdirilmiş
    // görünürlük göstəricisi — real HOS uyğunluq sistemi deyil).
    double remainingDrivingHours;
    boolean fatigueWarning;
    // Bu sürücünün son reysində (istənilən reysində) həll olunmamış DVIR
    // defekti varmı (bax DvirInspectionRepository).
    boolean hasUnresolvedDvirDefect;
    Double ratingAverage;
    Integer ratingCount;
}
