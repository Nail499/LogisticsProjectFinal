package com.ltc.logisticsproject.dto.payment;

import com.ltc.logisticsproject.entity.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Tam faktura görünüşü — Payment + Cargo + Customer məlumatını birləşdirir
// (bax PaymentService#buildInvoice). Üç ayrı controller-dən (customer,
// dispatcher, admin) eyni formada qaytarılır ki, frontend tərəfdə TƏK bir
// InvoiceView komponenti bütün rollarda təkrar istifadə oluna bilsin.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDetail {
    String invoiceNumber;
    Long paymentId;
    Long cargoId;
    String trackingNumber;

    String customerName;
    String customerPhone;
    String customerEmail;
    String customerCompany;

    String description;
    String cargoType;
    Double weight;
    Double volume;
    String pickupAddress;
    String destinationAddress;

    Double amount;
    String currency;
    PaymentStatus status;
    String createdAt;
    String paidAt;
}
