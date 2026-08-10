package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.DvirType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DvirSubmitRequest {
    DvirType type;
    Map<String, String> items;
    String notes;
}
