package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.payment.AdminPaymentView;
import com.ltc.logisticsproject.dto.payment.InvoiceDetail;
import com.ltc.logisticsproject.dto.payment.OfflinePaymentRequest;
import com.ltc.logisticsproject.entity.Payment;
import com.ltc.logisticsproject.service.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Dispetçer panelindəki "Ödənişlər" bölməsi (bax frontend
// DispatcherPayments.jsx) — Admin-in payments görünüşü ilə eyni məlumat
// (bax PaymentService#listAllPaymentViews, AdminPaymentController-lə
// paylaşılan məntiq). Ayrıca controller lazımdır, çünki SecurityConfig-də
// /api/admin/** yalnız ADMIN roluna açıqdır, /api/dispatcher/** isə
// DISPATCHER + ADMIN-ə (bax DispatcherController-in eyni konvensiyası).
@RestController
@RequestMapping("/api/dispatcher/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatcherPaymentController {

    final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<AdminPaymentView>> all() {
        return ResponseEntity.ok(paymentService.listAllPaymentViews());
    }

    @GetMapping("/{paymentId}/invoice")
    public ResponseEntity<InvoiceDetail> invoice(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getInvoiceForStaff(paymentId));
    }

    // Dispetçerin özü yaratdığı "zəngli sifariş" (real Customer hesabı
    // olmayan, bax Cargo.customer/customerName) ödəniş mərhələsində əbədi
    // qalmasın deyə — pul artıq telefonla/əl-ələ (nağd, bank köçürməsi)
    // alınıbsa, dispetçer bunu burada qeydə alır (bax
    // PaymentService#recordOfflinePayment izahı, frontend
    // CustomerInfoModal.jsx "Ödənişi qeydə al" düyməsi).
    @PostMapping("/cargo/{cargoId}/offline")
    public ResponseEntity<Payment> recordOffline(@PathVariable Long cargoId, @RequestBody(required = false) OfflinePaymentRequest request) {
        String note = request != null ? request.getNote() : null;
        return ResponseEntity.ok(paymentService.recordOfflinePayment(cargoId, note));
    }
}
