package com.ltc.logisticsproject.entity;

public enum CargoStatus {
    PENDING,
    ASSIGNED,
    IN_TRANSIT,
    DELIVERED,
    // Dispetçer "Gözləyən yüklər" siyahısından xoşuna gəlməyən/qəbul etmək
    // istəmədiyi yükü imtina edəndə (bax DispatcherController#rejectCargo)
    // — real silmə (DB-dən) əvəzinə status dəyişdirilir ki, müştəri öz
    // sifariş tarixçəsində "Ləğv edildi" kimi görsün.
    CANCELLED
}
