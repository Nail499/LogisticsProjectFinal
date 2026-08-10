package com.ltc.logisticsproject.dto.chat;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageResponse {
    Long id;
    Long cargoId;
    Long senderUserId;
    String senderName;
    String senderRole;
    String message;
    String imageUrl;
    String createdAt;
    // Hazırkı sorğunu edən istifadəçinin öz mesajı olub-olmadığı — frontend
    // bunu bulle-nin sağda/solda göstərilməsi üçün istifadə edir.
    boolean mine;
}
