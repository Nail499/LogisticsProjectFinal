package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.ExpenseCategory;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Public-safe view of a TripExpense for PublicTrackingResponse — the raw
// entity carries a full Trip back-reference (driver/vehicle/cargos) that we
// don't want serialized into the public tracking payload, so we flatten just
// the fields the customer actually needs to see (see request: "yol boyu
// xərclər" + şübhəli/anomaly ones should be visible to the customer too, not
// just admin).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripExpenseView {
    Long id;
    ExpenseCategory category;
    Double amount;
    String description;
    Boolean isAnomaly;
    String recordedAt;
}
