package com.ltc.logisticsproject.entity;

// Beynəlxalq logistikada yükün hansı nəqliyyat növü ilə daşındığını bildirir.
// Fleetra əsasən avtomobil (TRUCK) daşımalarına qurulub, digərləri (dəmiryol,
// dəniz, hava) real dünyada Azərbaycanın Orta Dəhliz/TRACECA marşrutlarında
// tez-tez multimodal şəkildə birləşir (məs. TRUCK + SEA Xəzər üzərindən).
public enum TransportMode {
    TRUCK, RAIL, SEA, AIR
}
