package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Sifariş (Cargo) üzrə müştəri ilə dispetçer/admin/sürücü arasında canlı
// yazışma — "otaq" açarı cargoId-dir (bir sifariş = bir söhbət). Real-vaxt
// çatdırılma STOMP broker vasitəsilə olur (bax ChatService,
// WebSocketConfig-dəki mövcud /topic broker), lakin YAZMA əməliyyatı
// qəsdən STOMP @MessageMapping (client->server) ilə yox, adi authenticated
// REST POST ilə edilir — /ws endpoint-i SecurityConfig-də permitAll
// olduğu üçün (GPS broadcast üçün açılıb) STOMP CONNECT üzərində JWT
// yoxlanmır; REST yolu ilə göndərən istifadəçinin kimliyi hər zaman
// Authentication-dan götürülür, client-in göndərdiyi ad/rol heç vaxt
// etibar edilmir (bax ChatService#sendMessage).
@Entity
@Table(name = "chat_messages")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Long cargoId;

    // CUSTOMER_DRIVER / CUSTOMER_DISPATCHER / INTERNAL — bax entity/ChatChannel
    // qeydi. Qəsdən nullable = false DEYİL: "chat_messages" cədvəlində
    // artıq sətirlər var idi, Postgres isə mövcud sətirləri olan cədvələ
    // DEFAULT-suz NOT NULL sütun əlavə etməyə icazə vermir ("column channel
    // contains null values" xətası) — Cargo.requiresCustoms-dakı eyni qərara
    // bax. Köhnə sətirlər (ilk versiyada tək "CUSTOMER" otağından qalan) DB-də
    // NULL qalır, oxuyanda CUSTOMER_DISPATCHER kimi rəftar olunur (bax
    // ChatService#effectiveChannel) — yeni yazılan sətirlər isə @PrePersist-də
    // CUSTOMER_DISPATCHER-ə sabitlənir (əgər çağıran tərəf channel göstərməyibsə).
    @Enumerated(EnumType.STRING)
    ChatChannel channel;

    @Column(nullable = false)
    Long senderUserId;

    @Column(nullable = false)
    String senderName;

    @Column(nullable = false)
    String senderRole;

    // Şəkil-yalnız mesajlarda boş qala bilər (bax imageUrl) — ona görə
    // artıq nullable = false deyil.
    @Column(length = 2000)
    String message;

    // Söhbətə göndərilən şəkil (bax ChatService#sendImage,
    // FileStorageService) — /uploads/** altında statik fayl kimi saxlanılır.
    String imageUrl;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.channel == null) this.channel = ChatChannel.CUSTOMER_DISPATCHER;
    }
}
