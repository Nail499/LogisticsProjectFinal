package com.ltc.logisticsproject.dto.customs;

import com.ltc.logisticsproject.entity.BorderCustomsStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BorderCrossingRequest {
    String borderPointName;
    String country;
    BorderCustomsStatus customsStatus;
    String notes;
}
