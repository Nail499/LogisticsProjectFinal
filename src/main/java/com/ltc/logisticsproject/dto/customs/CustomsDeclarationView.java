package com.ltc.logisticsproject.dto.customs;

import com.ltc.logisticsproject.entity.CustomsDeclaration;
import com.ltc.logisticsproject.entity.DeclarationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomsDeclarationView {
    String declarationNumber;
    String originCountry;
    String destinationCountry;
    Double declaredValue;
    String currency;
    Double dutyAmount;
    Double vatAmount;
    Double totalPayable;
    DeclarationStatus status;
    String submittedAt;
    String clearedAt;

    public static CustomsDeclarationView from(CustomsDeclaration d) {
        return CustomsDeclarationView.builder()
                .declarationNumber(d.getDeclarationNumber())
                .originCountry(d.getOriginCountry())
                .destinationCountry(d.getDestinationCountry())
                .declaredValue(d.getDeclaredValue())
                .currency(d.getCurrency())
                .dutyAmount(d.getDutyAmount())
                .vatAmount(d.getVatAmount())
                .totalPayable(d.getTotalPayable())
                .status(d.getStatus())
                .submittedAt(d.getSubmittedAt() != null ? d.getSubmittedAt().toString() : null)
                .clearedAt(d.getClearedAt() != null ? d.getClearedAt().toString() : null)
                .build();
    }
}
