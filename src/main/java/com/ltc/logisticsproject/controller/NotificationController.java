package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.entity.Notification;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.repository.NotificationRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Saytın yuxarısındakı zəng ikonu bu endpoint-ləri çağırır. Qəsdən
// /api/admin, /api/dispatcher və s. altında DEYİL — bütün rollar (SecurityConfig-də
// xüsusi qayda yoxdur, ona görə .anyRequest().authenticated() altına düşür,
// yəni istənilən login olmuş istifadəçi öz bildirişlərinə çata bilir).
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationController {

    final NotificationRepository notificationRepository;
    final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> list(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return ResponseEntity.ok(Map.of("count", notificationRepository.countByUserIdAndReadFalse(userId)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = currentUserId(authentication);
        Notification n = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Bildiriş tapılmadı"));
        n.setRead(true);
        notificationRepository.save(n);
        return ResponseEntity.ok(n);
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        Long userId = currentUserId(authentication);
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok(Map.of("message", "Hamısı oxunmuş kimi qeyd edildi"));
    }

    private Long currentUserId(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
        return user.getId();
    }
}
