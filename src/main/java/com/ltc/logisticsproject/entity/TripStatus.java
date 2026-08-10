package com.ltc.logisticsproject.entity;

public enum TripStatus {
    // Dispetçer reysi yaradıb, amma yükün qiyməti hələ müştəri tərəfindən
    // ödənilməyib — sürücüyə HƏLƏ göndərilmir. Bütün yüklər ödəndikdən sonra
    // avtomatik PENDING_ACCEPTANCE-a keçir (bax PaymentService#confirmPayment).
    // Qiymət təyin olunmayan yüklər bu mərhələni keçib birbaşa
    // PENDING_ACCEPTANCE ilə yaradılır (bax DispatcherService#createTrip).
    AWAITING_PAYMENT,
    // Reys sürücüyə göndərilib, sürücü hələ qəbul/imtina etməyib — bax
    // DispatcherService#createTrip, DriverTripService#acceptTrip.
    PENDING_ACCEPTANCE,
    PLANNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    // Sürücü reysi imtina edib — yüklər avtomatik PENDING statusuna qaytarılır
    // ki, dispetçer başqa sürücüyə təhkim edə bilsin (bax
    // DriverTripService#rejectTrip).
    REJECTED,
    // Dispetçer sürücünün/müştərinin cavabını gözləmədən reysi özü ləğv edib
    // (məs. sürücü uzun müddət qəbul/imtina etmirsə) — yalnız hələ AWAITING_
    // PAYMENT/PENDING_ACCEPTANCE mərhələsindəki reyslər üçün mümkündür (bax
    // DispatcherService#cancelTrip). REJECTED-dən fərqi: bu, sürücünün deyil,
    // DİSPETÇERİN qərarıdır.
    CANCELLED
}
