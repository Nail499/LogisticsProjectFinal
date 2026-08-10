package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// AI dəstək chat-inin bir mesajı — role: "user" | "assistant". Söhbət tarixçəsi
// backend-də SAXLANILMIR (stateless) — frontend hər sorğuda bütün tarixçəni
// göndərir (bax SupportChatWidget.jsx), server hər dəfə Groq API-yə
// tam kontekstlə müraciət edir. Sadəliyi seçilib: ayrıca DB cədvəli/təmizləmə
// məntiqi tələb etmir, söhbətlər adətən qısa olur.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SupportChatMessage {
    String role;
    String content;
}
