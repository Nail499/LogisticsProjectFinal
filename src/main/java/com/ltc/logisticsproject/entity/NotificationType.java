package com.ltc.logisticsproject.entity;

public enum NotificationType {
    WELCOME,
    ORDER_CREATED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    PASSWORD_CHANGED,
    PAYMENT_RECEIVED,
    NEW_CHAT_MESSAGE,
    NEW_ORDER,
    ORDER_CANCELLED,
    GENERAL,
    // Reys qəbul/imtina axını — bax NotificationService#notifyTripAssigned/
    // notifyTripRejected.
    TRIP_ASSIGNED,
    TRIP_REJECTED,
    // Dispetçer sürücünün cavabını gözləmədən reysi ləğv edəndə sürücüyə
    // gedən bildiriş — bax NotificationService#notifyTripCancelledByDispatcher.
    TRIP_CANCELLED,
    // Sürücü yolda fövqəladə hal bildirəndə dispetçer/admin-lərə gedən
    // təcili bildiriş — bax NotificationService#notifyIncidentReported.
    INCIDENT_REPORTED,
    // Reys öncəsi/sonrası yoxlama siyahısında (DVIR) defekt qeyd olunanda
    // dispetçer/admin-lərə gedən bildiriş — bax
    // NotificationService#notifyDvirDefect.
    DVIR_DEFECT
}
