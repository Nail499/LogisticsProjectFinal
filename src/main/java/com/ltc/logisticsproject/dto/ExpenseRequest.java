package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.ExpenseCategory;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseRequest {
    ExpenseCategory category;
    Double amount;
    String description;
}