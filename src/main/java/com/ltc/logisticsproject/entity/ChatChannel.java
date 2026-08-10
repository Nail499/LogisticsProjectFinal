package com.ltc.logisticsproject.entity;

// Bir cargoId altında ÜÇ ayrı söhbət otağı — heç biri digərini "eşitmir":
//   CUSTOMER_DRIVER     — müştəri <-> sürücü (dispetçer/admin nəzarət üçün görə bilər)
//   CUSTOMER_DISPATCHER — müştəri <-> dispetçer/admin (sürücü daxil deyil)
//   INTERNAL            — sürücü <-> dispetçer/admin (müştəri heç vaxt görmür)
// (bax ChatService#requireAccess). Əvvəlcə tək "CUSTOMER" otağı var idi
// (müştəri+sürücü+dispetçer+admin hamısı bir yerdə) — istifadəçi müştərinin
// sürücü və dispetçerlə AYRI-AYRI yazışa bilməsini istədi, ona görə ikiyə
// bölündü.
public enum ChatChannel {
    CUSTOMER_DRIVER,
    CUSTOMER_DISPATCHER,
    INTERNAL
}
