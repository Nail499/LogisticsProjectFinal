package com.ltc.logisticsproject.entity;

// Beynəlxalq ticarətdə tərəflər arasında xərc/risk bölgüsünü müəyyən edən
// standart müqavilə şərtləri (ICC Incoterms). Yalnız ən çox istifadə olunan
// altı şərt daxil edilib — tam siyahı (11 şərt) real biznesdə nadir hallarda
// tam istifadə olunur.
public enum Incoterm {
    EXW,  // Ex Works — alıcı bütün daşıma məsuliyyətini öz üzərinə götürür
    FCA,  // Free Carrier
    FOB,  // Free On Board — dəniz daşımaları üçün
    CIF,  // Cost, Insurance & Freight
    CPT,  // Carriage Paid To
    DAP,  // Delivered At Place
    DDP   // Delivered Duty Paid — satıcı gömrük rüsumunu da ödəyir
}
