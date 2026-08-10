package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.AdminSummaryResponse;
import com.ltc.logisticsproject.dto.AnomalyExpenseResponse;
import com.ltc.logisticsproject.dto.DispatcherAnalyticsResponse;
import com.ltc.logisticsproject.service.AdminReportService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminReportController {

    final AdminReportService adminReportService;

    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> summary() {
        return ResponseEntity.ok(adminReportService.getSummary());
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<AnomalyExpenseResponse>> anomalies() {
        return ResponseEntity.ok(adminReportService.getAnomalies());
    }

    @GetMapping("/routes/top")
    public ResponseEntity<Map<String, Long>> topRoutes() {
        return ResponseEntity.ok(adminReportService.getTopRoutes());
    }

    @GetMapping("/analytics")
    public ResponseEntity<DispatcherAnalyticsResponse> analytics() {
        return ResponseEntity.ok(adminReportService.getMonthlyAnalytics());
    }
}
