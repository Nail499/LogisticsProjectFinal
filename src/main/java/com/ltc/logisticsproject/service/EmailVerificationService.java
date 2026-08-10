package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.entity.VerificationPurpose;
import com.ltc.logisticsproject.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

// Qeydiyyat email-təsdiqi VƏ "şifrəni unutdum" kodu — hər ikisi eyni
// mexanizmi paylaşır: 6 rəqəmli, SecureRandom-la yaradılan, 15 dəqiqə
// etibarlı kod User.verificationCode/-ExpiresAt-da saxlanılır.
// (JobApplication.applicationCode/Cargo.trackingNumber-dəki
// System.currentTimeMillis() pattern-i burada YETƏRLİ DEYİL — o yalnız
// unikallıq üçün idi, bu isə təhlükəsizlik-həssas bir kod olduğu üçün əsl
// təsadüfilik (SecureRandom) tələb edir.)
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailVerificationService {

    private static final int CODE_TTL_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    final UserRepository userRepository;
    final EmailService emailService;

    // Yeni 6 rəqəmli kod yaradır, User-ə yazır və email-ə göndərir.
    public void generateAndSend(User user, String toEmail, VerificationPurpose purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
        user.setVerificationPurpose(purpose);
        userRepository.save(user);
        emailService.sendVerificationCode(toEmail, code, purpose);
    }

    // Kodu yoxlayır (mövcudluq, məqsəd, vaxt, uyğunluq) — uğurlu olarsa kodu
    // dərhal təmizləyir ki, təkrar istifadə oluna bilməsin. Uğursuz olarsa
    // aydın Azərbaycanca mesajla RuntimeException atır (bax bu kod bazasında
    // hər yerdə istifadə olunan pattern — heç bir @ExceptionHandler yoxdur,
    // mesaj birbaşa frontend-ə err.response.data.message kimi çatır).
    public void verify(User user, String code, VerificationPurpose purpose) {
        if (user.getVerificationCode() == null || user.getVerificationPurpose() != purpose) {
            throw new RuntimeException("Kod tapılmadı — yenidən göndərməyi cəhd edin");
        }
        if (user.getVerificationCodeExpiresAt() == null || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Kodun vaxtı bitib — yenisini tələb edin");
        }
        if (!user.getVerificationCode().equals(code)) {
            throw new RuntimeException("Kod yanlışdır");
        }
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        user.setVerificationPurpose(null);
        userRepository.save(user);
    }
}
