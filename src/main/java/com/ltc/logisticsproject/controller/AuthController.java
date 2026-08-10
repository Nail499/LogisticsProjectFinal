package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.auth.ForgotPasswordRequest;
import com.ltc.logisticsproject.dto.auth.LoginRequest;
import com.ltc.logisticsproject.dto.auth.LoginResponse;
import com.ltc.logisticsproject.dto.auth.RegisterCustomerRequest;
import com.ltc.logisticsproject.dto.auth.ResendCodeRequest;
import com.ltc.logisticsproject.dto.auth.ResetPasswordRequest;
import com.ltc.logisticsproject.dto.auth.VerifyCodeRequest;
import com.ltc.logisticsproject.entity.Customer;
import com.ltc.logisticsproject.entity.NotificationType;
import com.ltc.logisticsproject.entity.Role;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.entity.VerificationPurpose;
import com.ltc.logisticsproject.repository.CustomerRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import com.ltc.logisticsproject.security.JwtUtil;
import com.ltc.logisticsproject.service.EmailVerificationService;
import com.ltc.logisticsproject.service.NotificationService;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthController {

    final AuthenticationManager authenticationManager;
    final UserRepository userRepository;
    final CustomerRepository customerRepository;
    final PasswordEncoder passwordEncoder;
    final JwtUtil jwtUtil;
    final EmailVerificationService emailVerificationService;
    final NotificationService notificationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Giriş həm username, həm email ilə mümkündür (bax resolveUsername).
        // Spring Security-nin AuthenticationManager-i yalnız username qəbul
        // edir, ona görə əvvəlcə real username-ə çeviririk, sonra normal
        // axınla davam edir.
        String actualUsername = resolveUsername(request.getUsername());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(actualUsername, request.getPassword())
            );
        } catch (DisabledException e) {
            // Qeydiyyatdan keçib, amma email kodu ilə hələ təsdiqlənməyib —
            // adi "yanlış şifrə" mesajı əvəzinə səbəbi aydın deyirik ki,
            // istifadəçi nə edəcəyini bilsin (bax CustomerRegister.jsx-dəki
            // kod addımı).
            throw new RuntimeException("Email ünvanınız hələ təsdiqlənməyib — qeydiyyat zamanı göndərilən kodu daxil edin");
        }

        User user = userRepository.findByUsername(actualUsername)
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        return ResponseEntity.ok(new LoginResponse(token, user.getRole().name(), user.getUsername()));
    }

    // "İstifadəçi adı və ya email" sahəsini real username-ə çevirir. Email
    // yalnız Customer entity-də olduğu üçün (Driver/Dispatcher/Admin üçün
    // User-də ayrıca email sahəsi yoxdur), bu, praktikada yalnız müştəri
    // hesabları üçün işləyir — digər rollar həmişə username ilə daxil olur,
    // heç nə dəyişmir. Nə username, nə email kimi tapılmasa, identifier-i
    // olduğu kimi qaytarır ki, authenticate() adi "yanlış istifadəçi
    // adı/şifrə" xətasını versin (hansı formatın səhv olduğunu sızdırmır).
    private String resolveUsername(String identifier) {
        if (identifier == null) return null;
        if (userRepository.findByUsername(identifier).isPresent()) {
            return identifier;
        }
        return customerRepository.findFirstByEmail(identifier)
                .flatMap(c -> userRepository.findByCustomerId(c.getId()))
                .map(User::getUsername)
                .orElse(identifier);
    }

    // Qeydiyyat artıq iki addımlıdır: 1) bu endpoint Customer+User (enabled=
    // false) yaradır və email-ə 6 rəqəmli təsdiq kodu göndərir; 2) istifadəçi
    // kodu /verify-email-ə göndərir, orada enabled=true olur və avtomatik
    // login edilir (bax CustomerRegister.jsx).
    @PostMapping("/register/customer")
    public ResponseEntity<?> registerCustomer(@RequestBody RegisterCustomerRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu istifadəçi adı artıq mövcuddur"));
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email ünvanı tələb olunur — təsdiq kodu ora göndəriləcək"));
        }
        if (!isStrongPassword(request.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", PASSWORD_RULE_MESSAGE));
        }

        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .companyName(request.getCompanyName())
                .build();
        customer = customerRepository.save(customer);

        User user = User.builder()
                .fullName(request.getFullName())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .customerId(customer.getId())
                .enabled(false)
                .build();
        user = userRepository.save(user);

        emailVerificationService.generateAndSend(user, request.getEmail(), VerificationPurpose.REGISTER);

        return ResponseEntity.ok(Map.of(
                "message", "Qeydiyyat qəbul edildi — email ünvanınıza göndərilən kodu daxil edin",
                "username", user.getUsername()
        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyCodeRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));

        emailVerificationService.verify(user, request.getCode(), VerificationPurpose.REGISTER);

        user.setEnabled(true);
        userRepository.save(user);

        // Hesab indi aktivləşdi — zəng ikonuna + email-ə "Xoş gəlmisiniz"
        // bildirişi. Customer.email VerifyCodeRequest-də yoxdur, ona görə
        // customerId üzərindən tapılır.
        String welcomeEmail = customerRepository.findById(user.getCustomerId())
                .map(Customer::getEmail).orElse(null);
        notificationService.notifyWithEmail(
                user.getId(), welcomeEmail, NotificationType.WELCOME,
                "Fleetra ailəsinə xoş gəlmisiniz!",
                "Hesabınız uğurla aktivləşdirildi. İndi sifariş yarada, göndərişlərinizi izləyə bilərsiniz.",
                "/customer", "Fleetra — xoş gəlmisiniz", "Panelə keç"
        );

        // Təsdiqdən sonra istifadəçini əlavə login addımı olmadan birbaşa
        // sistemə buraxırıq — daha rəvan qeydiyyat təcrübəsi.
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new LoginResponse(token, user.getRole().name(), user.getUsername()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody ResendCodeRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
        if (Boolean.TRUE.equals(user.getEnabled())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu hesab artıq təsdiqlənib"));
        }
        Customer customer = customerRepository.findById(user.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Müştəri tapılmadı"));
        emailVerificationService.generateAndSend(user, customer.getEmail(), VerificationPurpose.REGISTER);
        return ResponseEntity.ok(Map.of("message", "Kod yenidən göndərildi"));
    }

    // Təhlükəsizlik qeydi: email sistemdə mövcud olub-olmamasından asılı
    // olmayaraq HƏMİŞƏ eyni ümumi mesajı qaytarır (istifadəçi siyahısının
    // email ünvanları ilə "sınanıb tapılmasın" — enumeration hücumu qarşısı).
    // Mövcud deyilsə sadəcə heç nə göndərilmir.
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        customerRepository.findFirstByEmail(request.getEmail()).ifPresent(customer -> {
            userRepository.findByCustomerId(customer.getId()).ifPresent(user ->
                    emailVerificationService.generateAndSend(user, customer.getEmail(), VerificationPurpose.PASSWORD_RESET)
            );
        });
        return ResponseEntity.ok(Map.of("message", "Əgər bu email ünvanı sistemdə mövcuddursa, kod ora göndərildi"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        Customer customer = customerRepository.findFirstByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Kod yanlışdır və ya vaxtı bitib"));
        User user = userRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new RuntimeException("Kod yanlışdır və ya vaxtı bitib"));

        emailVerificationService.verify(user, request.getCode(), VerificationPurpose.PASSWORD_RESET);

        if (!isStrongPassword(request.getNewPassword())) {
            throw new RuntimeException(PASSWORD_RULE_MESSAGE);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Şifrə uğurla dəyişəndə təsdiq email-i — həm "əməliyyat tamamlandı"
        // bildirişi, həm də təhlükəsizlik siqnalı (əgər bu dəyişikliyi
        // istifadəçi özü etməyibsə, dərhal xəbər tutsun).
        notificationService.notifyWithEmail(
                user.getId(), customer.getEmail(), NotificationType.PASSWORD_CHANGED,
                "Şifrəniz dəyişdirildi",
                "Fleetra hesabınızın şifrəsi uğurla yeniləndi. Əgər bu dəyişikliyi siz etməmisinizsə, dərhal bizimlə əlaqə saxlayın.",
                null, "Fleetra — şifrəniz dəyişdirildi", null
        );

        return ResponseEntity.ok(Map.of("message", "Şifrəniz uğurla yeniləndi — indi daxil ola bilərsiniz"));
    }

    // Frontend-dəki eyni qaydanı (bax frontend/src/utils/passwordRules.js)
    // güzgüləyir — orada yalnız UX üçün canlı göstərici var, əsl mühafizə
    // burdadır: ən azı 8 simvol, 1 böyük hərf, 1 kiçik hərf, 1 rəqəm.
    static final String PASSWORD_RULE_MESSAGE =
            "Şifrə ən azı 8 simvol olmalı, ən azı 1 böyük hərf, 1 kiçik hərf və 1 rəqəm ehtiva etməlidir";

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        return hasUpper && hasLower && hasDigit;
    }
}
