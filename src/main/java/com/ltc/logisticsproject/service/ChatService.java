package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.chat.ChatCargoSummary;
import com.ltc.logisticsproject.dto.chat.ChatMessageResponse;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.CargoRepository;
import com.ltc.logisticsproject.repository.ChatMessageRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Sifariş üzrə canlı yazışma — bax entity/ChatMessage üzərindəki qeyd
// (niyə REST POST + STOMP broadcast, STOMP @MessageMapping yox). Hər
// cargoId altında ÜÇ ayrı otaq var (bax ChatChannel): CUSTOMER_DRIVER —
// müştəri <-> sürücü; CUSTOMER_DISPATCHER — müştəri <-> dispetçer/admin;
// INTERNAL — sürücü <-> dispetçer/admin (müştəri heç vaxt görmür). Admin/
// dispetçer həmişə bütün otaqlara giriş hüququna malikdir (nəzarət üçün).
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatService {

    final ChatMessageRepository chatMessageRepository;
    final CargoRepository cargoRepository;
    final UserRepository userRepository;
    final SimpMessagingTemplate messagingTemplate;
    final FileStorageService fileStorageService;
    // Yeni mesaj gələndə qarşı tərəfə (müştəri/sürücü/dispetçer) zəng ikonu +
    // push bildirişi göndərmək üçün — bax notifyParticipants aşağıda.
    final NotificationService notificationService;

    public List<ChatMessageResponse> getHistory(Long cargoId, ChatChannel channel, Authentication authentication) {
        Cargo cargo = cargoRepository.findById(cargoId).orElseThrow(() -> new RuntimeException("Sifariş tapılmadı"));
        User currentUser = requireAccess(cargo, channel, authentication);
        // Filtr burada, DB sorğusunda yox — bax repository qeydi: köhnə
        // sətirlərdə channel sütunu NULL ola bilər, bunlar CUSTOMER_DISPATCHER
        // kimi rəftar olunur.
        return chatMessageRepository.findByCargoIdOrderByCreatedAtAsc(cargoId).stream()
                .filter(m -> effectiveChannel(m) == channel)
                .map(m -> toResponse(m, currentUser.getId()))
                .toList();
    }

    private ChatChannel effectiveChannel(ChatMessage m) {
        return m.getChannel() != null ? m.getChannel() : ChatChannel.CUSTOMER_DISPATCHER;
    }

    // Dispetçer/admin/sürücü panellərindəki mərkəzi "Yazışma" bölməsi üçün
    // (bax ChatCargoController, frontend ChatHub.jsx) — hansı sifarişlər
    // haqqında yazışa biləcəyi rola görə dəyişir: admin/dispetçer bütün
    // sifarişləri, sürücü yalnız özünə təhkim olunmuş reyslərin yüklərini,
    // müştəri isə yalnız öz sifarişlərini görür.
    public List<ChatCargoSummary> listChatCargos(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
        List<Cargo> cargos = switch (user.getRole()) {
            case ADMIN, DISPATCHER -> cargoRepository.findAllByOrderByIdDesc();
            case DRIVER -> cargoRepository.findAllByOrderByIdDesc().stream()
                    .filter(c -> c.getTrip() != null && c.getTrip().getDriver() != null
                            && c.getTrip().getDriver().getId().equals(user.getDriverId()))
                    .toList();
            case CUSTOMER -> cargoRepository.findByCustomerId(user.getCustomerId());
        };
        return cargos.stream().map(this::toCargoSummary).toList();
    }

    private ChatCargoSummary toCargoSummary(Cargo c) {
        boolean hasDriver = c.getTrip() != null && c.getTrip().getDriver() != null;
        return ChatCargoSummary.builder()
                .cargoId(c.getId())
                .trackingNumber(c.getTrackingNumber())
                .description(c.getDescription())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .customerName(c.getCustomer() != null ? c.getCustomer().getFullName() : c.getCustomerName())
                .driverName(hasDriver ? c.getTrip().getDriver().getFullName() : null)
                .hasDriver(hasDriver)
                .build();
    }

    public ChatMessageResponse sendMessage(Long cargoId, ChatChannel channel, String text, Authentication authentication) {
        if (text == null || text.isBlank()) {
            throw new RuntimeException("Mesaj boş ola bilməz");
        }
        Cargo cargo = cargoRepository.findById(cargoId).orElseThrow(() -> new RuntimeException("Sifariş tapılmadı"));
        User currentUser = requireAccess(cargo, channel, authentication);

        ChatMessage message = ChatMessage.builder()
                .cargoId(cargoId)
                .channel(channel)
                .senderUserId(currentUser.getId())
                .senderName(displayName(currentUser))
                .senderRole(currentUser.getRole().name())
                .message(text.trim())
                .build();
        return persistAndBroadcast(message, cargo, currentUser);
    }

    // Söhbətə şəkil göndərmək (bax FileStorageService — eyni /uploads/**
    // saxlama mexanizmi profil/nəqliyyat vasitəsi şəkilləri üçün istifadə
    // olunan). Mesaj mətni yoxdur, yalnız imageUrl dolur.
    public ChatMessageResponse sendImage(Long cargoId, ChatChannel channel, MultipartFile image, Authentication authentication) {
        if (image == null || image.isEmpty()) {
            throw new RuntimeException("Şəkil seçilməyib");
        }
        Cargo cargo = cargoRepository.findById(cargoId).orElseThrow(() -> new RuntimeException("Sifariş tapılmadı"));
        User currentUser = requireAccess(cargo, channel, authentication);

        String imageUrl = fileStorageService.store(image);
        ChatMessage message = ChatMessage.builder()
                .cargoId(cargoId)
                .channel(channel)
                .senderUserId(currentUser.getId())
                .senderName(displayName(currentUser))
                .senderRole(currentUser.getRole().name())
                .imageUrl(imageUrl)
                .build();
        return persistAndBroadcast(message, cargo, currentUser);
    }

    private ChatMessageResponse persistAndBroadcast(ChatMessage message, Cargo cargo, User currentUser) {
        message = chatMessageRepository.save(message);

        ChatMessageResponse payload = toResponse(message, null);
        // Broadcast-da "mine" hər zaman false göndərilir — alan tərəf STOMP
        // mesajını öz user ID-si ilə müqayisə edərək özü təyin edir (bax
        // frontend/src/components/OrderChat.jsx). Hər 3 kanal üçün AYRI
        // mövzular (topic) istifadə olunur ki, məs. müştərinin brauzeri
        // INTERNAL yayımına ümumiyyətlə abunə olmasın (bax OrderChat.jsx-də
        // channel-ə görə mövzu seçimi).
        messagingTemplate.convertAndSend("/topic/chat/" + message.getCargoId() + topicSuffix(message.getChannel()), payload);

        try {
            notifyParticipants(cargo, currentUser, message, message.getChannel());
        } catch (Exception e) {
            // Bildiriş uğursuz olsa da mesajın özü artıq yadda saxlanıb və
            // yayımlanıb — burada dayandırmırıq.
        }

        return toResponse(message, currentUser.getId());
    }

    // Hər kanal üçün ayrı STOMP mövzusu — bax persistAndBroadcast/OrderChat.jsx.
    private String topicSuffix(ChatChannel channel) {
        return switch (channel) {
            case CUSTOMER_DRIVER -> "/driver";
            case CUSTOMER_DISPATCHER -> "/dispatcher";
            case INTERNAL -> "/internal";
        };
    }

    // CUSTOMER_DRIVER: sifarişin müştərisinə və təhkim olunmuş sürücüyə —
    // göndərənin özü istisna. CUSTOMER_DISPATCHER: müştəriyə (göndərən
    // dispetçer/admin-dirsə), müştəri yazıbsa isə bütün dispetçer/admin
    // komandasına. INTERNAL: sürücü yazıbsa komandaya, dispetçer/admin
    // yazıbsa sürücüyə. Heç bir halda dispetçer/admin CUSTOMER_DRIVER və ya
    // CUSTOMER_DISPATCHER üçün ayrıca bildiriş almır — istənilən vaxt
    // panellərindən bütün söhbətlərə baxa bilirlər, hər mesajda bildiriş
    // lazımsız spam yaradardı.
    private void notifyParticipants(Cargo cargo, User sender, ChatMessage message, ChatChannel channel) {
        String textForPreview = message.getMessage() != null ? message.getMessage() : "📷 Şəkil göndərildi";
        String preview = textForPreview.length() > 100 ? textForPreview.substring(0, 100) + "…" : textForPreview;
        String title = "Yeni mesaj — " + displayName(sender);

        switch (channel) {
            case CUSTOMER_DRIVER -> {
                notifyCustomer(cargo, sender, title, preview);
                notifyDriver(cargo, sender, title, preview);
            }
            case CUSTOMER_DISPATCHER -> {
                notifyCustomer(cargo, sender, title, preview);
                if (sender.getRole() == Role.CUSTOMER) {
                    notificationService.notifyDispatchers(title, preview, "/dispatcher/queue");
                }
            }
            case INTERNAL -> {
                if (sender.getRole() == Role.DRIVER) {
                    notificationService.notifyDispatchers(title, preview, "/dispatcher/trips");
                } else {
                    notifyDriver(cargo, sender, title, preview);
                }
            }
        }
    }

    private void notifyCustomer(Cargo cargo, User sender, String title, String preview) {
        if (cargo.getCustomer() == null) return;
        userRepository.findByCustomerId(cargo.getCustomer().getId())
                .filter(u -> !u.getId().equals(sender.getId()))
                .ifPresent(u -> notificationService.notify(
                        u.getId(), NotificationType.NEW_CHAT_MESSAGE, title, preview, "/customer/orders"));
    }

    private void notifyDriver(Cargo cargo, User sender, String title, String preview) {
        if (cargo.getTrip() == null || cargo.getTrip().getDriver() == null) return;
        userRepository.findByDriverId(cargo.getTrip().getDriver().getId())
                .filter(u -> !u.getId().equals(sender.getId()))
                .ifPresent(u -> notificationService.notify(
                        u.getId(), NotificationType.NEW_CHAT_MESSAGE, title, preview, "/driver"));
    }

    // Bu sifarişin hansı otağına kimlərin girişi var — bax ChatChannel qeydi.
    // Admin/dispetçer HƏR ÜÇ otağa girə bilir (nəzarət). Müştəri yalnız öz
    // iki otağına (CUSTOMER_DRIVER, CUSTOMER_DISPATCHER), sürücü yalnız öz
    // iki otağına (CUSTOMER_DRIVER, INTERNAL) — heç biri o birinin
    // "özəl" otağına (müştəri: INTERNAL, sürücü: CUSTOMER_DISPATCHER) girə bilmir.
    private User requireAccess(Cargo cargo, ChatChannel channel, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));

        boolean driverAssigned = cargo.getTrip() != null && cargo.getTrip().getDriver() != null
                && cargo.getTrip().getDriver().getId().equals(user.getDriverId());
        boolean ownsCargo = cargo.getCustomer() != null && cargo.getCustomer().getId().equals(user.getCustomerId());

        boolean allowed = switch (channel) {
            case CUSTOMER_DRIVER -> switch (user.getRole()) {
                case ADMIN, DISPATCHER -> true;
                case CUSTOMER -> ownsCargo;
                case DRIVER -> driverAssigned;
            };
            case CUSTOMER_DISPATCHER -> switch (user.getRole()) {
                case ADMIN, DISPATCHER -> true;
                case CUSTOMER -> ownsCargo;
                case DRIVER -> false;
            };
            case INTERNAL -> switch (user.getRole()) {
                case ADMIN, DISPATCHER -> true;
                case DRIVER -> driverAssigned;
                case CUSTOMER -> false;
            };
        };

        if (!allowed) {
            throw new RuntimeException("Bu söhbətə giriş icazəniz yoxdur");
        }
        return user;
    }

    private String displayName(User user) {
        return switch (user.getRole()) {
            case ADMIN -> "Admin";
            case DISPATCHER -> user.getFullName() != null ? user.getFullName() : "Dispetçer";
            case DRIVER -> user.getFullName() != null ? user.getFullName() : "Sürücü";
            case CUSTOMER -> user.getFullName() != null ? user.getFullName() : "Müştəri";
        };
    }

    private ChatMessageResponse toResponse(ChatMessage m, Long currentUserId) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .cargoId(m.getCargoId())
                .senderUserId(m.getSenderUserId())
                .senderName(m.getSenderName())
                .senderRole(m.getSenderRole())
                .message(m.getMessage())
                .imageUrl(m.getImageUrl())
                .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                .mine(currentUserId != null && currentUserId.equals(m.getSenderUserId()))
                .build();
    }
}
