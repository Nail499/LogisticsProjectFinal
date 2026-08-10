package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.ExpenseCategory;
import com.ltc.logisticsproject.entity.TripStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

// Admin panelinin "Şübhəli xərclər" səhifəsi üçün — bare TripExpense (yalnız
// trip.id) əvəzinə, xərcin ARDINDAKI kontekstin hamısını daşıyır: hansı reys,
// hansı sürücü, hansı müştəri(lər), hansı marşrut. trackingNumber məhz buna
// görə var — admin "Ətraflı bax" düyməsini klikləyəndə artıq mövcud olan
// TripDetailModal (bax frontend/src/components/TripDetailModal.jsx, eyni
// /api/tracking/{trackingNumber} endpoint-i sürücü/dispetçer/müştəri
// panellərində də istifadə edir) tam xəritəni/marşrutu açır — bu DTO xəritə
// məlumatını təkrarlamır, sadəcə açmaq üçün açar verir.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnomalyExpenseResponse {
    Long id;
    ExpenseCategory category;
    Double amount;
    String description;
    String recordedAt;

    // Sürücünün yüklədiyi qəbz fotosu (könüllü) — admin/dispetçer şübhəli
    // xərci yoxlayanda görsün deyə (bax DriverController#addExpenseWithReceipt).
    String receiptPhotoUrl;

    // "Nə üçün şübhəlidir" izahı — həmin kateqoriyanın orta dəyəri və bu
    // xərcin ondan neçə faiz yuxarı olduğu (bax ExpenseService#detectAnomaly
    // — mean + 2*stdDev həddini keçəndə şübhəli sayılır).
    Double categoryAverageAmount;
    Double percentAboveAverage;

    Long tripId;
    TripStatus tripStatus;
    String trackingNumber;
    String driverName;
    String vehiclePlate;
    String pickupAddress;
    String destinationAddress;
    List<CustomerSummary> customers;
}
