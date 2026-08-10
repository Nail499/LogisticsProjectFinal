package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.chat.ChatMessageRequest;
import com.ltc.logisticsproject.dto.chat.ChatMessageResponse;
import com.ltc.logisticsproject.entity.ChatChannel;
import com.ltc.logisticsproject.service.ChatService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Sifariş üzrə canlı söhbət — bax ChatService (giriş icazəsi, real-vaxt
// yayım, CUSTOMER_DRIVER/CUSTOMER_DISPATCHER/INTERNAL otaq ayrımı).
// "/api/chat/**" heç bir rol prefiksinə uyğun gəlmir, ona görə
// SecurityConfig-də .anyRequest().authenticated() altına düşür — dəqiq
// giriş nəzarəti (kim hansı otağa girə bilər) ChatService#requireAccess-də
// edilir. "channel" query param-ı MƏCBURİDİR (default yoxdur) — hər çağıran
// tərəf hansı otağı nəzərdə tutduğunu açıq göstərməlidir.
@RestController
@RequestMapping("/api/chat/cargo/{cargoId}/messages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatController {

    final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> history(
            @PathVariable Long cargoId,
            @RequestParam ChatChannel channel,
            Authentication authentication) {
        return ResponseEntity.ok(chatService.getHistory(cargoId, channel, authentication));
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> send(
            @PathVariable Long cargoId,
            @RequestParam ChatChannel channel,
            @RequestBody ChatMessageRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(chatService.sendMessage(cargoId, channel, request.getMessage(), authentication));
    }

    @PostMapping(value = "/image", consumes = "multipart/form-data")
    public ResponseEntity<ChatMessageResponse> sendImage(
            @PathVariable Long cargoId,
            @RequestParam ChatChannel channel,
            @RequestParam("image") MultipartFile image,
            Authentication authentication) {
        return ResponseEntity.ok(chatService.sendImage(cargoId, channel, image, authentication));
    }
}
