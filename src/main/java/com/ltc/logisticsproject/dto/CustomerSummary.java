package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.Cargo;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Lightweight, public-safe view of "who this cargo/trip belongs to" — used
// wherever the dispatcher needs to click a customer name and see contact
// details (Control Tower map, "Bütün reyslər", backhaul matcher), not just
// the original "Gözləyən yüklər" queue. Built from Cargo rather than
// returning the raw Customer entity so unregistered (phone-in, see
// DispatcherCargoRequest) orders — which have no linked Customer row, just
// plain customerName/customerPhone text — still produce a usable card.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerSummary {
    String fullName;
    String phone;
    String email;
    String companyName;
    Boolean registered;
    String trackingNumber;
    // Canlı söhbət otağı açarı (bax ChatController/OrderChat.jsx) —
    // trackingNumber-dən fərqli olaraq heç vaxt dəyişmir, ona görə chat
    // üçün daha etibarlıdır.
    Long cargoId;

    // Dispetçer panelindəki "Müştəri məlumatları" pəncərəsində (bax
    // CustomerInfoModal.jsx) ödəniş vəziyyətinin görünməsi üçün — sifariş
    // hələ dispetçer tərəfindən qəbul olunmayıbsa (PENDING) price adətən
    // null olur, frontend bunu "Qiymət hələ təyin olunmayıb" kimi göstərir.
    Double price;
    Boolean paid;

    public static CustomerSummary from(Cargo cargo) {
        if (cargo.getCustomer() != null) {
            return CustomerSummary.builder()
                    .fullName(cargo.getCustomer().getFullName())
                    .phone(cargo.getCustomer().getPhone())
                    .email(cargo.getCustomer().getEmail())
                    .companyName(cargo.getCustomer().getCompanyName())
                    .registered(true)
                    .trackingNumber(cargo.getTrackingNumber())
                    .cargoId(cargo.getId())
                    .price(cargo.getPrice())
                    .paid(cargo.getPaid())
                    .build();
        }
        return CustomerSummary.builder()
                .fullName(cargo.getCustomerName())
                .phone(cargo.getCustomerPhone())
                .registered(false)
                .trackingNumber(cargo.getTrackingNumber())
                .cargoId(cargo.getId())
                .price(cargo.getPrice())
                .paid(cargo.getPaid())
                .build();
    }
}
