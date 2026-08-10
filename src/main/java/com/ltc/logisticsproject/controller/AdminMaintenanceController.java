package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.KeptAccountPreview;
import com.ltc.logisticsproject.service.MaintenanceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Bir dəfəlik "test datasını sıfırla" əməliyyatı (bax MaintenanceService,
// frontend AdminMaintenance.jsx). Qəsdən əsas admin naviqasiyasına
// ƏLAVƏ OLUNMUR (bax AdminLayout.jsx) — yalnız birbaşa /admin/maintenance
// ünvanından əlçatandır ki, təsadüfən klikə məruz qalmasın. SecurityConfig-də
// /api/admin/** artıq hasRole("ADMIN") tələb edir.
@RestController
@RequestMapping("/api/admin/maintenance")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminMaintenanceController {

    final MaintenanceService maintenanceService;

    @GetMapping("/preview")
    public ResponseEntity<List<KeptAccountPreview>> preview() {
        return ResponseEntity.ok(maintenanceService.previewKeptAccounts());
    }

    @PostMapping("/reset-test-data")
    public ResponseEntity<Map<String, Integer>> resetTestData() {
        return ResponseEntity.ok(maintenanceService.resetTestData());
    }

    // Panel boş görsənməsin deyə bir dəfəlik nümunə sürücü/tır/qoşqu
    // datası yaradır (bax MaintenanceService#seedDemoData) — YÜK/REYS
    // yaratmır, bu ayrıca istəniləcək.
    @PostMapping("/seed-demo-data")
    public ResponseEntity<Map<String, Integer>> seedDemoData() {
        return ResponseEntity.ok(maintenanceService.seedDemoData());
    }

    // resetTestData()-dan sonra saxlanılan DISPATCHER + CUSTOMER hesabını
    // da silir — ADMIN, sürücülər, tırlar/qoşqular toxunulmaz qalır (bax
    // MaintenanceService#wipeDispatcherAndCustomerAccounts).
    @PostMapping("/wipe-dispatcher-customer")
    public ResponseEntity<Map<String, Integer>> wipeDispatcherAndCustomerAccounts() {
        return ResponseEntity.ok(maintenanceService.wipeDispatcherAndCustomerAccounts());
    }
}
