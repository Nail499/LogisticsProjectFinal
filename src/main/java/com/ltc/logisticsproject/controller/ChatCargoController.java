package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.chat.ChatCargoSummary;
import com.ltc.logisticsproject.service.ChatService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Dispetçer/admin/sürücü panellərindəki mərkəzi "Yazışma" səhifəsi üçün —
// müştərinin CustomerChat.jsx-də istifadə etdiyi eyni modelin bu rollara da
// açılması (bax ChatService#listChatCargos, frontend ChatHub.jsx). Ayrıca
// controller saxlanılıb ki, ChatController-in
// "/api/chat/cargo/{cargoId}/messages" path şablonu ilə toqquşmasın.
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatCargoController {

    final ChatService chatService;

    @GetMapping("/cargo-list")
    public ResponseEntity<List<ChatCargoSummary>> cargoList(Authentication authentication) {
        return ResponseEntity.ok(chatService.listChatCargos(authentication));
    }
}
