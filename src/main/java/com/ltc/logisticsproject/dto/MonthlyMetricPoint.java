package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Generic "label -> value" point used to feed Recharts widgets on the
// Dispatcher Control Tower (Stage 4): monthly expense totals and estimated
// monthly carbon footprint both reuse this same tiny shape.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MonthlyMetricPoint {
    String label;
    Double value;
}
