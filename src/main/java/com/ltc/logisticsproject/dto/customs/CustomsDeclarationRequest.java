package com.ltc.logisticsproject.dto.customs;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomsDeclarationRequest {
    String originCountry;
    String destinationCountry;
    String hsCode;
    Double declaredValue;
    String currency;
}
