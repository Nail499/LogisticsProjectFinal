package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.SupportChatRequest;
import com.ltc.logisticsproject.dto.SupportChatResponse;
import com.ltc.logisticsproject.entity.Role;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.repository.UserRepository;
import com.ltc.logisticsproject.service.AiChatService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// AI dəstək chat-i — Müştəri/Sürücü/Dispetçer panellərindəki üzən chat
// widget-i (bax frontend SupportChatWidget.jsx) buraya sorğu göndərir.
// Admin panelində göstərilmir (admin onsuz da tam sistem girişinə malikdir,
// istifadəçi seçimi ilə bu 3 rolla məhdudlaşdırıldı). Path /api/support-chat
// SecurityConfig-də ayrıca rol-prefiksinə (dispatcher/driver/customer) düşmür,
// ona görə .anyRequest().authenticated() qaydasına düşür — hər rol buraya
// müraciət edə bilər, faktiki icazə/scoping burada, kod daxilində edilir.
@RestController
@RequestMapping("/api/support-chat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SupportChatController {

    final UserRepository userRepository;
    final AiChatService aiChatService;

    @PostMapping
    public ResponseEntity<SupportChatResponse> chat(@RequestBody SupportChatRequest request, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));

        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("AI dəstək chat-i admin panelində mövcud deyil");
        }

        Long roleEntityId = switch (user.getRole()) {
            case CUSTOMER -> user.getCustomerId();
            case DRIVER -> user.getDriverId();
            case DISPATCHER -> user.getId();
            default -> null;
        };

        String reply = aiChatService.chat(user.getRole(), roleEntityId, request.getMessages());
        return ResponseEntity.ok(SupportChatResponse.builder().reply(reply).build());
    }
}
