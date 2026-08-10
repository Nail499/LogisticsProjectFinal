package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import com.ltc.logisticsproject.util.CsvUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Admin hesabatlarının CSV (Excel-də birbaşa açıla bilən) ixracı — bax
// util/CsvUtil. JWT header tələb olunduğu üçün (bax SecurityConfig)
// frontend bu endpoint-lərə sadə <a href> ilə yox, axios ilə
// responseType:'blob' çağırıb Blob URL yaradaraq endirmə tetikləyir
// (bax frontend/src/utils/csvExport.js).
@RestController
@RequestMapping("/api/admin/export")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminExportController {

    final TripRepository tripRepository;
    final PaymentRepository paymentRepository;
    final CargoRepository cargoRepository;
    final AuditLogRepository auditLogRepository;

    @GetMapping("/trips.csv")
    public ResponseEntity<String> exportTrips() {
        List<String> headers = List.of(
                "Reys ID", "Status", "Sürücü", "Tır", "Yaradılıb", "Götürülüb", "Çatdırılıb",
                "Məsafə (km)", "Təxmini xərc", "Yük sayı", "Tracking №-lər"
        );
        List<List<String>> rows = new ArrayList<>();
        List<Trip> trips = tripRepository.findAll().stream()
                .sorted(Comparator.comparing(Trip::getId).reversed())
                .toList();
        for (Trip t : trips) {
            List<Cargo> cargos = cargoRepository.findByTripId(t.getId());
            String trackingNumbers = cargos.stream().map(Cargo::getTrackingNumber).reduce((a, b) -> a + "; " + b).orElse("");
            List<String> row = new ArrayList<>();
            row.add(str(t.getId()));
            row.add(str(t.getStatus()));
            row.add(t.getDriver() != null ? str(t.getDriver().getFullName()) : "");
            row.add(t.getVehicle() != null ? str(t.getVehicle().getPlateNumber()) : "");
            row.add(str(t.getCreatedAt()));
            row.add(str(t.getStartedAt()));
            row.add(str(t.getDeliveredAt()));
            row.add(str(t.getEstimatedDistanceKm()));
            row.add(str(t.getEstimatedCost()));
            row.add(String.valueOf(cargos.size()));
            row.add(trackingNumbers);
            rows.add(row);
        }
        return csvResponse("reysler.csv", headers, rows);
    }

    @GetMapping("/payments.csv")
    public ResponseEntity<String> exportPayments() {
        List<String> headers = List.of("Ödəniş ID", "Tracking №", "Müştəri", "Məbləğ", "Valyuta", "Status", "Yaradılıb", "Ödənilib");
        List<List<String>> rows = new ArrayList<>();
        List<Payment> payments = paymentRepository.findAll().stream()
                .sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
                .toList();
        for (Payment p : payments) {
            Cargo cargo = cargoRepository.findById(p.getCargoId()).orElse(null);
            List<String> row = new ArrayList<>();
            row.add(str(p.getId()));
            row.add(cargo != null ? str(cargo.getTrackingNumber()) : "");
            row.add(cargo != null ? str(cargo.getCustomerName()) : "");
            row.add(str(p.getAmount()));
            row.add(str(p.getCurrency()));
            row.add(str(p.getStatus()));
            row.add(str(p.getCreatedAt()));
            row.add(str(p.getPaidAt()));
            rows.add(row);
        }
        return csvResponse("odenisler.csv", headers, rows);
    }

    @GetMapping("/audit-logs.csv")
    public ResponseEntity<String> exportAuditLogs() {
        List<String> headers = List.of("Vaxt", "İstifadəçi", "Rol", "Əməliyyat", "Detallar");
        List<List<String>> rows = new ArrayList<>();
        for (AuditLog a : auditLogRepository.findTop200ByOrderByCreatedAtDesc()) {
            List<String> row = new ArrayList<>();
            row.add(str(a.getCreatedAt()));
            row.add(str(a.getActorUsername()));
            row.add(str(a.getActorRole()));
            row.add(str(a.getAction()));
            row.add(str(a.getDetails()));
            rows.add(row);
        }
        return csvResponse("fealiyyet-tarixcesi.csv", headers, rows);
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private ResponseEntity<String> csvResponse(String filename, List<String> headers, List<List<String>> rows) {
        String csv = CsvUtil.toCsv(headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
