package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogResponse {
    Long id;
    String actorUsername;
    String actorRole;
    String action;
    String entityType;
    String details;
    String createdAt;
}
