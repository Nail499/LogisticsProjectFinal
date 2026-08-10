package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.payment.AdminPaymentView;
import com.ltc.logisticsproject.dto.payment.InvoiceDetail;
import com.ltc.logisticsproject.dto.payment.PaymentIntentResponse;
import com.ltc.logisticsproject.dto.payment.PaymentResponse;
import com.ltc.logisticsproject.dto.payment.PublishableKeyResponse;
import com.ltc.logisticsproject.entity.Cargo;
import com.ltc.logisticsproject.entity.Customer;
import com.ltc.logisticsproject.entity.Payment;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.repository.CargoRepository;
import com.ltc.logisticsproject.repository.CustomerRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import com.ltc.logisticsproject.service.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Müştərinin sifariş üçün Stripe ilə ödəniş etməsi (bax PaymentService).
// Frontend axını: (1) GET /config ilə publishable key alınır, (2) POST
// /cargo/{id}/payment-intent ilə clientSecret alınıb Stripe Elements
// kart formasına verilir, (3) kart təsdiqləndikdən sonra POST
// /payments/{id}/confirm ilə backend Stripe-dan statusu yenidən yoxlayıb
// Cargo.paid=true edir (müştəri tərəfindən göndərilən statusa etibar
// edilmir — həmişə Stripe-ın özündən təsdiqlənir).
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerPaymentController {

    final PaymentService paymentService;
    final CargoRepository cargoRepository;
    final UserRepository userRepository;
    final CustomerRepository customerRepository;

    @GetMapping("/payments/config")
    public ResponseEntity<PublishableKeyResponse> config() {
        return ResponseEntity.ok(PublishableKeyResponse.builder()
                .publishableKey(paymentService.getPublishableKey())
                .build());
    }

    @PostMapping("/cargo/{cargoId}/payment-intent")
    public ResponseEntity<PaymentIntentResponse> createIntent(@PathVariable Long cargoId, Authentication authentication) {
        Customer customer = currentCustomer(authentication);
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new RuntimeException("Sifariş tapılmadı"));
        if (cargo.getCustomer() == null || !cargo.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Bu sifarişə giriş icazəniz yoxdur");
        }

        Payment payment = paymentService.createPaymentIntent(cargo, customer);

        return ResponseEntity.ok(PaymentIntentResponse.builder()
                .paymentId(payment.getId())
                .clientSecret(payment.getClientSecret())
                .publishableKey(paymentService.getPublishableKey())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .build());
    }

    @PostMapping("/payments/{paymentId}/confirm")
    public ResponseEntity<PaymentResponse> confirm(@PathVariable Long paymentId, Authentication authentication) {
        Customer customer = currentCustomer(authentication);
        Payment payment = paymentService.confirmPayment(paymentId, customer.getId());
        return ResponseEntity.ok(toResponse(payment));
    }

    // Müştərinin "Fakturalarım" səhifəsi (bax frontend CustomerInvoices.jsx)
    // — öz ödəniş/faktura tarixçəsi. Admin/dispetçer "Ödənişlər" siyahısı
    // ilə eyni sətir forması (AdminPaymentView) istifadə olunur ki, tracking
    // № də görünsün (bax PaymentService#listPaymentViewsForCustomer).
    @GetMapping("/payments")
    public ResponseEntity<List<AdminPaymentView>> myPayments(Authentication authentication) {
        Customer customer = currentCustomer(authentication);
        return ResponseEntity.ok(paymentService.listPaymentViewsForCustomer(customer.getId()));
    }

    @GetMapping("/payments/{paymentId}/invoice")
    public ResponseEntity<InvoiceDetail> invoice(@PathVariable Long paymentId, Authentication authentication) {
        Customer customer = currentCustomer(authentication);
        return ResponseEntity.ok(paymentService.getInvoiceForCustomer(paymentId, customer.getId()));
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .cargoId(p.getCargoId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .paidAt(p.getPaidAt() != null ? p.getPaidAt().toString() : null)
                .build();
    }

    private Customer currentCustomer(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
        return customerRepository.findById(user.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Müştəri tapılmadı"));
    }
}
