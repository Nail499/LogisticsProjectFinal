package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.entity.Cargo;
import com.ltc.logisticsproject.entity.CustomsTariff;
import com.ltc.logisticsproject.repository.CustomsTariffRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

// Gömrük rüsumu + ƏDV hesablama məntiqi. Real dövlət gömrük sistemi ilə
// inteqrasiya deyil — admin-in idarə etdiyi CustomsTariff cədvəlinə əsaslanan,
// sistemin öz daxili (amma real riyazi məntiqlə işləyən) hesablamasıdır.
//
// Məntiq:
//   1) Cargo.cargoType-a görə CustomsTariff cədvəlindən rüsum/ƏDV faizi axtarılır.
//      Tapılmasa, Azərbaycanda tipik dəyərlərə yaxın defolt faizlər istifadə olunur.
//   2) dutyAmount = declaredValue * dutyRate / 100
//   3) ƏDV, mal dəyəri + rüsum üzərindən hesablanır (real gömrük praktikasında
//      olduğu kimi — ƏDV bazası "gömrük dəyəri + rüsum"dur):
//      vatAmount = (declaredValue + dutyAmount) * vatRate / 100
//   4) totalPayable = dutyAmount + vatAmount (gömrüyə ödəniləcək məbləğ,
//      malın öz dəyəri deyil)
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomsDutyService {

    final CustomsTariffRepository customsTariffRepository;

    // Azərbaycanda idxal ƏDV-si standart 18%-dir (Vergi Məcəlləsi). Rüsum
    // faizi mal növündən çox asılı olduğundan tarif cədvəlində konfiqurasiya
    // olunur; heç bir sətir tapılmasa 5% ümumi (GENERAL) dərəcəsi tətbiq edilir.
    public static final double DEFAULT_DUTY_RATE_PERCENT = 5.0;
    public static final double DEFAULT_VAT_RATE_PERCENT = 18.0;

    public record DutyCalculation(double dutyRatePercent, double vatRatePercent,
                                   double dutyAmount, double vatAmount, double totalPayable) {
    }

    public DutyCalculation calculate(Cargo cargo, double declaredValue) {
        return calculate(cargo.getCargoType(), declaredValue);
    }

    // Müştəri kalkulyatoru üçün — hələ heç bir Cargo/sifariş yaradılmadan,
    // sadəcə mal növü + dəyər əsasında təxmini hesablama aparmaq lazımdır
    // (bax CustomerCargoController#estimateCustoms).
    public DutyCalculation calculate(com.ltc.logisticsproject.entity.CargoType cargoType, double declaredValue) {
        double dutyRate = DEFAULT_DUTY_RATE_PERCENT;
        double vatRate = DEFAULT_VAT_RATE_PERCENT;

        if (cargoType != null) {
            CustomsTariff tariff = customsTariffRepository.findByCargoType(cargoType).orElse(null);
            if (tariff != null) {
                dutyRate = tariff.getDutyRatePercent();
                vatRate = tariff.getVatRatePercent();
            }
        }

        double dutyAmount = round2(declaredValue * dutyRate / 100.0);
        double vatAmount = round2((declaredValue + dutyAmount) * vatRate / 100.0);
        double totalPayable = round2(dutyAmount + vatAmount);

        return new DutyCalculation(dutyRate, vatRate, dutyAmount, vatAmount, totalPayable);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
