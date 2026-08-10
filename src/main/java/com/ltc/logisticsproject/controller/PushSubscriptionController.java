package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.push.FcmTokenRequest;
import com.ltc.logisticsproject.dto.push.PushSubscribeRequest;
import com.ltc.logisticsproject.dto.push.PushUnsubscribeRequest;
import com.ltc.logisticsproject.entity.FcmToken;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.repository.FcmTokenRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import com.ltc.logisticsproject.service.PushNotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Brauzer push bildirişi abunəlik idarəetməsi — bütün rollar üçün ortaq
// (istənilən istifadəçi öz bildirişlərinə abunə ola bilər), ona görə
// "/api/customer/**" və s. kimi rol-prefiksli deyil, SecurityConfig-də
// .anyRequest().authenticated() altına düşür.
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushSubscriptionController {

    final PushNotificationService pushNotificationService;
    final UserRepository userRepository;
    final FcmTokenRepository fcmTokenRepository;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> vapidPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", pushNotificationService.getPublicKey()));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody PushSubscribeRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        pushNotificationService.subscribe(user.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Abunəlik qeydə alındı"));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody PushUnsubscribeRequest request) {
        pushNotificationService.unsubscribe(request.getEndpoint());
        return ResponseEntity.ok(Map.of("message", "Abunəlik ləğv edildi"));
    }

    // Mobil (Android) tərəf üçün — brauzer Web Push (yuxarıdakı /subscribe,
    // VAPID p256dh/auth ilə) FCM ilə uyumsuzdur, ona görə tamam ayrı, sadə
    // bir cədvəl/endpoint (bax entity/FcmToken.java). QEYD: hazırda yalnız
    // tokeni saxlayır — real FCM göndərişi Firebase Admin SDK + xidmət
    // hesabı açarı tələb edir, bu hələ qoşulmayıb (bax
    // PushNotificationService-dəki şərh).
    @PostMapping("/fcm-subscribe")
    public ResponseEntity<?> registerFcmToken(@RequestBody FcmTokenRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        if (fcmTokenRepository.findByToken(request.getToken()).isEmpty()) {
            fcmTokenRepository.save(FcmToken.builder()
                    .userId(user.getId())
                    .token(request.getToken())
                    .build());
        }
        return ResponseEntity.ok(Map.of("message", "Token qeydə alındı"));
    }

    @PostMapping("/fcm-unsubscribe")
    public ResponseEntity<?> unregisterFcmToken(@RequestBody FcmTokenRequest request) {
        fcmTokenRepository.deleteByToken(request.getToken());
        return ResponseEntity.ok(Map.of("message", "Token silindi"));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
    }
}
