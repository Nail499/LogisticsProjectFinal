package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.AuditLogResponse;
import com.ltc.logisticsproject.entity.AuditLog;
import com.ltc.logisticsproject.repository.AuditLogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Admin "Fəaliyyət tarixçəsi" səhifəsi — son 200 audit qeydini göstərir
// (bax AuditLogService, AdminManagementController-dəki çağırış nöqtələri).
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminAuditLogController {

    final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> all() {
        List<AuditLogResponse> result = auditLogRepository.findTop200ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .actorUsername(a.getActorUsername())
                .actorRole(a.getActorRole())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .details(a.getDetails())
                .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
                .build();
    }
}
