package com.ltc.logisticsproject.entity;

// Tır (Vehicle) və Qoşqunun (Trailer) kimə məxsus olduğunu ayırmaq üçün —
// istifadəçinin real filo strukturunda iki tip var: şirkətin özünə məxsus
// avadanlıq (istənilən aktiv sürücüyə dispetçer tərəfindən reys-be-reys
// verilə bilər) və sürücünün ÖZ tırı/qoşqusu (yalnız o sürücü ilə işləyir,
// başqa heç kimə təklif olunmur — bax Vehicle.driver/Trailer.driver
// OneToOne əlaqəsi ilə eynilikdə istifadə olunur).
public enum OwnerType {
    COMPANY,
    DRIVER_OWNED
}
