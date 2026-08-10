package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // Kanala görə filtr bilərəkdən burada DEYİL (bax ChatService#getHistory) —
    // "channel = 'CUSTOMER'" sorğusu DB-də channel=NULL olan köhnə sətirləri
    // tutmazdı (bax entity/ChatMessage-dəki nullable qeydi), ona görə filtr
    // tətbiq səviyyəsində, NULL-u CUSTOMER kimi rəftar edərək aparılır.
    List<ChatMessage> findByCargoIdOrderByCreatedAtAsc(Long cargoId);
}
