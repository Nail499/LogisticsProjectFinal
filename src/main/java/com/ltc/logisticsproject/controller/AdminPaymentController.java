package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.payment.AdminPaymentView;
import com.ltc.logisticsproject.dto.payment.InvoiceDetail;
import com.ltc.logisticsproject.service.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Admin üçün bütün ödənişlərin/fakturaların siyahısı — sadə "faktura
// sistemi" görünüşü: hər Payment sətri Cargo-nun tracking №-si və
// müştəri adı ilə zənginləşdirilir (bax PaymentService#listAllPaymentViews
// — DispatcherPaymentController ilə eyni məntiq paylaşılır). SecurityConfig-də
// /api/admin/** artıq hasRole("ADMIN") tələb edir.
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminPaymentController {

    final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<AdminPaymentView>> all() {
        return ResponseEntity.ok(paymentService.listAllPaymentViews());
    }

    @GetMapping("/{paymentId}/invoice")
    public ResponseEntity<InvoiceDetail> invoice(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getInvoiceForStaff(paymentId));
    }
}
