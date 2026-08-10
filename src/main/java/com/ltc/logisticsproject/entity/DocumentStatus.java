package com.ltc.logisticsproject.entity;

public enum DocumentStatus {
    PENDING,   // yükləndi, hələ yoxlanmayıb
    VERIFIED,  // dispetçer/admin sənədi düzgün hesab edib
    REJECTED   // sənəd yanlış/əskikdir, yenidən yüklənməlidir
}
