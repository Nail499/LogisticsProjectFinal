package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.payment.AdminPaymentView;
import com.ltc.logisticsproject.dto.payment.InvoiceDetail;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.CargoRepository;
import com.ltc.logisticsproject.repository.PaymentRepository;
import com.ltc.logisticsproject.repository.TripRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// Stripe test rejimi ilə real ödəniş axını (bax .idea/workspace.xml-dəki
// STRIPE_SECRET_KEY/STRIPE_PUBLISHABLE_KEY env dəyişənləri, eyni pattern
// DB_PASSWORD/MAIL_PASSWORD kimi). Stripe hazırda AZN valyutasını dəstəkləmir,
// ona görə göstərilən qiymət (AZN) saxlanılır, lakin faktiki PaymentIntent
// "usd" valyutası ilə eyni ədədi məbləğlə yaradılır — test rejimində real
// pul hərəkət etmir, ona görə bu sadələşdirmə kifayətdir.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentService {

    static final String CURRENCY = "usd";

    final PaymentRepository paymentRepository;
    final CargoRepository cargoRepository;
    final UserRepository userRepository;
    final PricingService pricingService;
    final NotificationService notificationService;
    // Ödəniş tamamlananda reysi AWAITING_PAYMENT-dən PENDING_ACCEPTANCE-a
    // keçirib sürücüyə göndərmək üçün (bax confirmPayment aşağıda).
    final TripRepository tripRepository;

    @Value("${stripe.secret-key}")
    String secretKey;

    @Value("${stripe.publishable-key}")
    String publishableKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    // Cargo üçün ödəniş niyyəti yaradır (və ya artıq PENDING olan varsa,
    // Stripe-dan cari statusunu yoxlayıb lazım gələrsə yenisini yaradır).
    // Məbləğ HƏMİŞƏ burada, serverdə yenidən hesablanır (bax PricingService)
    // — müştəri tərəfindən göndərilə biləcək məbləğə etibar edilmir.
    public Payment createPaymentIntent(Cargo cargo, Customer customer) {
        if (Boolean.TRUE.equals(cargo.getPaid())) {
            throw new RuntimeException("Bu sifariş artıq ödənilib");
        }

        double amount = cargo.getPrice() != null ? cargo.getPrice() : pricingService.calculatePrice(cargo);
        long amountCents = Math.round(amount * 100);

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(CURRENCY)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putMetadata("cargoId", String.valueOf(cargo.getId()))
                    .putMetadata("trackingNumber", cargo.getTrackingNumber())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Payment payment = Payment.builder()
                    .cargoId(cargo.getId())
                    .customerId(customer.getId())
                    .amount(amount)
                    .currency(CURRENCY)
                    .stripePaymentIntentId(intent.getId())
                    .status(PaymentStatus.PENDING)
                    .method("STRIPE")
                    .build();

            payment = paymentRepository.save(payment);
            payment.setClientSecret(intent.getClientSecret());
            return payment;
        } catch (StripeException e) {
            throw new RuntimeException("Ödəniş başlada bilmədi: " + e.getMessage());
        }
    }

    public Payment confirmPayment(Long paymentId, Long customerId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Ödəniş tapılmadı"));
        if (!payment.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Bu ödənişə giriş icazəniz yoxdur");
        }
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return payment;
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
            if ("succeeded".equals(intent.getStatus())) {
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);

                Cargo cargo = cargoRepository.findById(payment.getCargoId()).orElse(null);
                if (cargo != null) {
                    cargo.setPaid(true);
                    cargoRepository.save(cargo);

                    Customer customer = cargo.getCustomer();
                    if (customer != null) {
                        userRepository.findByCustomerId(customer.getId()).ifPresent(user ->
                                notificationService.notifyWithEmail(
                                        user.getId(), customer.getEmail(), NotificationType.PAYMENT_RECEIVED,
                                        "Ödənişiniz uğurla tamamlandı",
                                        cargo.getTrackingNumber() + " nömrəli sifariş üçün " + payment.getAmount() + " ödənişiniz qəbul edildi. Faktura hesabınızda mövcuddur.",
                                        "/customer/orders", "Fleetra — ödəniş qəbul edildi", "Sifarişə bax"
                                )
                        );
                    }

                    // Reys qəbul/imtina + "əvvəlcə ödəniş" axını: bu yükün bağlı olduğu
                    // reys hələ AWAITING_PAYMENT-dədirsə (sürücüyə göndərilməyib) və
                    // reysdəki QİYMƏTİ OLAN bütün digər yüklər də artıq ödənilibsə,
                    // reys indi sürücüyə göndərilir (bax
                    // DispatcherService#createTrip — reys yaradılanda niyə əvvəlcə
                    // AWAITING_PAYMENT olduğu izah olunub).
                    forwardTripToDriverIfFullyPaid(cargo);
                }
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new RuntimeException("Ödəniş hələ tamamlanmayıb (status: " + intent.getStatus() + ")");
            }
        } catch (StripeException e) {
            throw new RuntimeException("Ödəniş statusu yoxlanıla bilmədi: " + e.getMessage());
        }

        return payment;
    }

    // Dispetçerin "zəngli sifariş" (walk-in, Cargo.customer == null, yalnız
    // customerName/customerPhone mətn kimi) üçün özü qeydə aldığı ödəniş —
    // bax DispatcherPaymentController#recordOffline. Belə sifarişlərdə real
    // Customer hesabı olmadığı üçün Stripe/onlayn kart axını (yuxarıdakı
    // createPaymentIntent, Customer-i MƏCBURİ parametr kimi tələb edir)
    // mümkün deyil — pul əslində telefonla/əl-ələ artıq alınıb (nağd, bank
    // köçürməsi və s.), dispetçer bunu sadəcə sistemə qeyd edir ki, reys
    // AWAITING_PAYMENT-də əbədi qalmasın və sürücüyə göndərilsin.
    public Payment recordOfflinePayment(Long cargoId, String note) {
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new RuntimeException("Yük tapılmadı"));
        if (Boolean.TRUE.equals(cargo.getPaid())) {
            throw new RuntimeException("Bu sifariş artıq ödənilib");
        }

        double amount = cargo.getPrice() != null ? cargo.getPrice() : pricingService.calculatePrice(cargo);
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .cargoId(cargo.getId())
                .customerId(cargo.getCustomer() != null ? cargo.getCustomer().getId() : null)
                .amount(amount)
                .currency(CURRENCY)
                // Stripe PaymentIntent yoxdur — unique constraint-i pozmadan
                // sətri fərqləndirmək üçün sintetik, təkrarsız id (bax
                // stripePaymentIntentId sahəsinin "unique=true" olduğu
                // Payment.java).
                .stripePaymentIntentId("OFFLINE-" + cargo.getId() + "-" + System.currentTimeMillis())
                .status(PaymentStatus.SUCCEEDED)
                .method("OFFLINE_DISPATCHER")
                .offlineNote(note != null && !note.isBlank() ? note.trim() : null)
                .paidAt(now)
                .build();
        payment = paymentRepository.save(payment);
        // Lambda-ya köçürülə bilməsi üçün effectively-final surət (yuxarıda
        // "payment = paymentRepository.save(payment)" ilə yenidən mənimsədilib,
        // ona görə özü artıq effectively final deyil — bax aşağıdakı
        // ifPresent lambda-sı).
        final Payment savedPayment = payment;

        cargo.setPaid(true);
        cargoRepository.save(cargo);

        Customer customer = cargo.getCustomer();
        if (customer != null) {
            userRepository.findByCustomerId(customer.getId()).ifPresent(user ->
                    notificationService.notifyWithEmail(
                            user.getId(), customer.getEmail(), NotificationType.PAYMENT_RECEIVED,
                            "Ödənişiniz uğurla tamamlandı",
                            cargo.getTrackingNumber() + " nömrəli sifariş üçün " + savedPayment.getAmount() + " ödənişiniz qəbul edildi. Faktura hesabınızda mövcuddur.",
                            "/customer/orders", "Fleetra — ödəniş qəbul edildi", "Sifarişə bax"
                    )
            );
        }

        forwardTripToDriverIfFullyPaid(cargo);
        return payment;
    }

    // Bu yükün bağlı olduğu reys AWAITING_PAYMENT-dədirsə və reysdəki
    // QİYMƏTİ OLAN bütün yüklər (bu yük daxil) artıq ödənilibsə, reysi
    // PENDING_ACCEPTANCE-a keçirir və sürücüyə bildiriş göndərir. Qiyməti
    // olmayan yüklər hesaba qatılmır (onlar üçün ödəniş heç vaxt baş
    // verməyəcək — bax DispatcherService#createTrip).
    private void forwardTripToDriverIfFullyPaid(Cargo cargo) {
        Trip trip = cargo.getTrip();
        if (trip == null || trip.getStatus() != TripStatus.AWAITING_PAYMENT) return;

        List<Cargo> tripCargos = cargoRepository.findByTripId(trip.getId());
        boolean allPaid = tripCargos.stream()
                .allMatch(c -> c.getPrice() == null || Boolean.TRUE.equals(c.getPaid()));
        if (!allPaid) return;

        trip.setStatus(TripStatus.PENDING_ACCEPTANCE);
        Trip savedTrip = tripRepository.save(trip);

        Driver driver = savedTrip.getDriver();
        if (driver == null) return;
        Cargo firstCargo = tripCargos.isEmpty() ? cargo : tripCargos.get(0);
        userRepository.findByDriverId(driver.getId()).ifPresent(user ->
                notificationService.notifyTripAssigned(
                        savedTrip, user.getId(), driver.getEmail(),
                        firstCargo.getPickupAddress(), firstCargo.getDestinationAddress()
                )
        );
    }

    // Admin/dispetçer "Ödənişlər" siyahısı — hər iki panel eyni məlumatı
    // göstərir (bax AdminPaymentController, DispatcherPaymentController),
    // ona görə tikinti məntiqi bir yerdə saxlanılır.
    public List<AdminPaymentView> listAllPaymentViews() {
        return paymentRepository.findAll().stream()
                .sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
                .map(this::toAdminView)
                .toList();
    }

    // Müştərinin "Fakturalarım" səhifəsi — eyni sətir formatı (tracking №
    // daxil olmaqla), sadəcə bir müştərinin öz ödənişləri ilə məhdudlaşır.
    public List<AdminPaymentView> listPaymentViewsForCustomer(Long customerId) {
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toAdminView)
                .toList();
    }

    private AdminPaymentView toAdminView(Payment p) {
        Cargo cargo = cargoRepository.findById(p.getCargoId()).orElse(null);
        return AdminPaymentView.builder()
                .id(p.getId())
                .cargoId(p.getCargoId())
                .trackingNumber(cargo != null ? cargo.getTrackingNumber() : null)
                .customerName(cargo != null ? cargo.getCustomerName() : null)
                .customerEmail(cargo != null && cargo.getCustomer() != null ? cargo.getCustomer().getEmail() : null)
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .method(p.getMethod() != null ? p.getMethod() : "STRIPE")
                .offlineNote(p.getOfflineNote())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .paidAt(p.getPaidAt() != null ? p.getPaidAt().toString() : null)
                .build();
    }

    // Müştərinin öz fakturasına baxması (bax CustomerPaymentController) —
    // sahiblik yoxlanılır, başqasının ödənişinə giriş verilmir.
    public InvoiceDetail getInvoiceForCustomer(Long paymentId, Long customerId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Ödəniş tapılmadı"));
        if (!payment.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Bu fakturaya giriş icazəniz yoxdur");
        }
        return buildInvoice(payment);
    }

    // Dispetçer/admin istənilən fakturaya baxa bilir (bax
    // DispatcherPaymentController, AdminPaymentController) — nəzarət
    // funksiyası, sahiblik yoxlanılmır.
    public InvoiceDetail getInvoiceForStaff(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Ödəniş tapılmadı"));
        return buildInvoice(payment);
    }

    // Faktura — qanunən bu əməliyyatın hər iki tərəfinə (alıcı: müştəri;
    // satıcı adından fəaliyyət göstərən: dispetçer/admin) aid olmalıdır, ona
    // görə üç ayrı controller-dən (customer/dispatcher/admin) çağırılır,
    // amma məzmunu HAMISI üçün eynidir — burada bir yerdə qurulur.
    private InvoiceDetail buildInvoice(Payment payment) {
        Cargo cargo = cargoRepository.findById(payment.getCargoId()).orElse(null);
        Customer customer = cargo != null ? cargo.getCustomer() : null;
        return InvoiceDetail.builder()
                .invoiceNumber("FLT-INV-" + String.format("%06d", payment.getId()))
                .paymentId(payment.getId())
                .cargoId(payment.getCargoId())
                .trackingNumber(cargo != null ? cargo.getTrackingNumber() : null)
                .customerName(customer != null ? customer.getFullName() : (cargo != null ? cargo.getCustomerName() : null))
                .customerPhone(customer != null ? customer.getPhone() : (cargo != null ? cargo.getCustomerPhone() : null))
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerCompany(customer != null ? customer.getCompanyName() : null)
                .description(cargo != null ? cargo.getDescription() : null)
                .cargoType(cargo != null && cargo.getCargoType() != null ? cargo.getCargoType().name() : null)
                .weight(cargo != null ? cargo.getWeight() : null)
                .volume(cargo != null ? cargo.getVolume() : null)
                .pickupAddress(cargo != null ? cargo.getPickupAddress() : null)
                .destinationAddress(cargo != null ? cargo.getDestinationAddress() : null)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null)
                .paidAt(payment.getPaidAt() != null ? payment.getPaidAt().toString() : null)
                .build();
    }
}
