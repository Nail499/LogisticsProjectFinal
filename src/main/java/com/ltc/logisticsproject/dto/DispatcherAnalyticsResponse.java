package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatcherAnalyticsResponse {
    List<MonthlyMetricPoint> monthlyExpenses;
    List<MonthlyMetricPoint> monthlyCarbonFootprintKg;
}
