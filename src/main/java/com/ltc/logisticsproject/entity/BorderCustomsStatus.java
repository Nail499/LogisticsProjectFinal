package com.ltc.logisticsproject.entity;

// Bir konkret sərhəd/gömrük məntəqəsindən keçid zamanı yükün statusu (bax
// BorderCrossing) — CustomsDeclaration-ın ümumi statusundan fərqli olaraq,
// bu, məhz həmin sərhəd nöqtəsindəki gömrük əməliyyatını əks etdirir.
public enum BorderCustomsStatus {
    PENDING,  // sərhədə çatıb, gömrük yoxlaması gözlənilir
    CLEARED,  // yoxlanıb, keçidə icazə verilib
    HELD      // əlavə yoxlama üçün saxlanılıb
}
