package com.ltc.logisticsproject.dto.customs;

import com.ltc.logisticsproject.entity.BorderCrossing;
import com.ltc.logisticsproject.entity.BorderCustomsStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BorderCrossingView {
    Long id;
    String borderPointName;
    String country;
    BorderCustomsStatus customsStatus;
    String crossedAt;

    public static BorderCrossingView from(BorderCrossing b) {
        return BorderCrossingView.builder()
                .id(b.getId())
                .borderPointName(b.getBorderPointName())
                .country(b.getCountry())
                .customsStatus(b.getCustomsStatus())
                .crossedAt(b.getCrossedAt() != null ? b.getCrossedAt().toString() : null)
                .build();
    }
}
