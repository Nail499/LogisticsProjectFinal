package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.entity.Cargo;
import com.ltc.logisticsproject.entity.UrgencyLevel;
import org.springframework.stereotype.Service;

// Sifariş qiymətinin (AZN) hesablanması — sadə, şəffaf formula: baza haqq +
// çəki/həcm əsaslı əlavə + təcililik əmsalı + gömrük xidməti haqqı.
// Qəsdən HƏM sifariş yaradılanda (CustomerCargoController), HƏM ödəniş
// zamanı (PaymentService) eyni metoddan istifadə olunur ki, müştəri
// tərəfindən göndərilə biləcək saxta məbləğə etibar edilməsin — ödəniş
// məbləği HƏMİŞƏ backend-də serverdə yenidən hesablanır.
@Service
public class PricingService {

    private static final double BASE_FEE = 15.0;
    private static final double PER_KG = 1.2;
    private static final double PER_CBM = 8.0;
    private static final double EXPRESS_MULTIPLIER = 1.4;
    private static final double CUSTOMS_FEE = 10.0;

    public double calculatePrice(Cargo cargo) {
        double weight = cargo.getWeight() != null ? cargo.getWeight() : 0.0;
        double volume = cargo.getVolume() != null ? cargo.getVolume() : 0.0;

        double price = BASE_FEE + (weight * PER_KG) + (volume * PER_CBM);

        if (cargo.getUrgency() == UrgencyLevel.EXPRESS) {
            price *= EXPRESS_MULTIPLIER;
        }
        if (Boolean.TRUE.equals(cargo.getRequiresCustoms())) {
            price += CUSTOMS_FEE;
        }

        return Math.round(price * 100.0) / 100.0;
    }
}
