package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// "Yük qaiməsi" (əmtəə-nəqliyyat qaiməsi) — yolda DYP/nəzarət yoxlaması
// zamanı sürücüdə olmalı sənəd (bax Nazirlər Kabinetinin 4 oktyabr 2023-cü
// il tarixli 355 saylı Qərarı — "Elektron əmtəə-nəqliyyat qaiməsi" və "Yük
// avtonəqliyyatı üçün elektron yol vərəqi"). Bu, kommersiya FAKTURASINDAN
// (bax InvoiceDetail) FƏRQLİ sənəddir — məbləğ/ödəniş məlumatı DAŞIMIR,
// yalnız yükün özünü (nə daşınır, kimdən-kimə, hansı ünvanlar arası)
// təsvir edir. Fleetra-da real "AYNA" inteqrasiyası yoxdur, bu sadələşdirilmiş
// tətbiqi eyni məzmunu göstərir (bax DriverTripService#getWaybill).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CargoWaybillDetail {
    String trackingNumber;
    String description;
    String cargoType;
    Double weight;
    Double volume;

    String senderName;
    String pickupAddress;

    String receiverName;
    String receiverPhone;
    String destinationAddress;

    String driverName;
    String vehiclePlate;
    String routeInfo;
    String createdAt;
}
