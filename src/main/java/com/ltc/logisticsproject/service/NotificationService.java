package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.entity.Cargo;
import com.ltc.logisticsproject.entity.DvirInspection;
import com.ltc.logisticsproject.entity.DvirType;
import com.ltc.logisticsproject.entity.IncidentType;
import com.ltc.logisticsproject.entity.Notification;
import com.ltc.logisticsproject.entity.NotificationType;
import com.ltc.logisticsproject.entity.Role;
import com.ltc.logisticsproject.entity.Trip;
import com.ltc.logisticsproject.entity.TripIncident;
import com.ltc.logisticsproject.entity.TripStatus;
import com.ltc.logisticsproject.repository.NotificationRepository;
import com.ltc.logisticsproject.repository.UserRepository;

import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Saytın yuxarısındakı zəng ikonu üçün mərkəzi bildiriş nöqtəsi — bütün
// rollar (müştəri/sürücü/dispetçer/admin) eyni Notification cədvəlini
// paylaşır, NotificationController isə hər zaman cari istifadəçinin öz
// bildirişlərini filtrləyir (bax entity/Notification.java).
//
// Hər hadisə üçün email göndərilmir (istifadəçini yormamaq üçün) — yalnız
// əhəmiyyətli mərhələlər (xoş gəlmisiniz, sifariş qəbulu, yük götürüldü,
// çatdırıldı) həm in-app, həm email kimi gedir; IN_TRANSIT kimi ara
// addımlar yalnız zəng ikonunda görünür.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationService {

    final NotificationRepository notificationRepository;
    final UserRepository userRepository;
    final EmailService emailService;
    final PushNotificationService pushNotificationService;

    @Value("${app.frontend.url}")
    String frontendUrl;

    // Yalnız zəng ikonunda görünsün (email yox) — məs. IN_TRANSIT kimi ara
    // status dəyişiklikləri.
    public void notify(Long userId, NotificationType type, String title, String message, String link) {
        if (userId == null) return;
        Notification n = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .build();
        notificationRepository.save(n);

        try {
            pushNotificationService.sendPush(userId, title, message, link, frontendUrl);
        } catch (Exception e) {
            // Push abunəliyi olmayan/etibarsız istifadəçilər üçün də in-app
            // bildiriş yuxarıda artıq yaddaşdadır — burada dayandırmırıq.
        }
    }

    // Zəng ikonu + email — əhəmiyyətli mərhələlər üçün (xoş gəlmisiniz,
    // sifariş qəbulu, yük götürüldü, çatdırıldı).
    public void notifyWithEmail(Long userId, String email, NotificationType type, String title, String message,
                                 String link, String emailSubject, String ctaLabel) {
        notify(userId, type, title, message, link);
        if (email == null || email.isBlank()) return;
        String ctaLink = link != null ? frontendUrl + link : null;
        try {
            emailService.sendNotificationEmail(email, emailSubject, title, message, link != null ? ctaLabel : null, ctaLink);
        } catch (Exception e) {
            // Email göndərilməsi uğursuz olsa belə (SMTP kəsintisi və s.),
            // in-app bildiriş artıq yaddaşdadır — istifadəçi zəng ikonunda
            // görəcək, ona görə burada axını dayandırmırıq (əks halda məs.
            // sifariş yaratma/status yeniləmə əməliyyatının özü uğursuz olar).
        }
    }

    // Yeni sifariş (müştəri özü verəndə) bütün aktiv dispetçer/admin
    // istifadəçilərinə zəng ikonu bildirişi — "Gözləyən yüklər" siyahısına
    // yeni iş düşdüyünü dərhal bilsinlər, əks halda səhifəni özləri əl ilə
    // yeniləyənə qədər xəbərsiz qalırdılar (bax CustomerCargoController#create).
    // Yalnız zəng ikonu (email yox) — hər sifarişdə bütün komandaya email
    // getsə lazımsız spam olardı.
    public void notifyDispatchers(String title, String message, String link) {
        userRepository.findByRole(Role.DISPATCHER)
                .forEach(u -> notify(u.getId(), NotificationType.NEW_ORDER, title, message, link));
        userRepository.findByRole(Role.ADMIN)
                .forEach(u -> notify(u.getId(), NotificationType.NEW_ORDER, title, message, link));
    }

    // Reys statusu dəyişəndə (bax DriverTripService#updateStatus) həmin
    // reysdəki hər bir yükün müştərisinə uyğun bildirişi göndərir.
    public void notifyCargoStatusChange(Cargo cargo, TripStatus newStatus) {
        if (cargo.getCustomer() == null) return;
        Long customerUserId = userRepository.findByCustomerId(cargo.getCustomer().getId())
                .map(u -> u.getId())
                .orElse(null);
        if (customerUserId == null) return;

        String email = cargo.getCustomer().getEmail();
        String tracking = cargo.getTrackingNumber();
        String link = "/customer/orders";

        switch (newStatus) {
            case PICKED_UP -> notifyWithEmail(
                    customerUserId, email, NotificationType.PICKED_UP,
                    "Yükünüz götürüldü",
                    tracking + " nömrəli göndərişiniz sürücü tərəfindən götürüldü və yola çıxdı.",
                    link, "Fleetra — yükünüz yoldadır", "Sifarişə bax"
            );
            case IN_TRANSIT -> notify(
                    customerUserId, NotificationType.IN_TRANSIT,
                    "Yükünüz yoldadır",
                    tracking + " nömrəli göndəriş hazırda daşınma mərhələsindədir.",
                    link
            );
            case DELIVERED -> notifyWithEmail(
                    customerUserId, email, NotificationType.DELIVERED,
                    "Yükünüz çatdırıldı",
                    tracking + " nömrəli göndərişiniz uğurla təhvil verildi. Bizi seçdiyiniz üçün təşəkkür edirik!",
                    link, "Fleetra — göndərişiniz çatdırıldı", "Sifarişə bax"
            );
            default -> { /* PLANNED üçün ayrıca bildiriş yoxdur */ }
        }
    }

    // Reys qəbul/imtina axını — dispetçer yeni reys yaradanda sürücüyə "Yeni
    // reys — qəbul et və ya imtina et" bildirişi (bax
    // DispatcherService#createTrip). Email də göndərilir ki, sürücü tətbiqi
    // açıq saxlamasa belə xəbərsiz qalmasın.
    public void notifyTripAssigned(Trip trip, Long driverUserId, String driverEmail, String pickup, String destination) {
        if (driverUserId == null) return;
        String message = "Sizə yeni reys (#" + trip.getId() + ") təhkim olundu"
                + (pickup != null && destination != null ? ": " + pickup + " → " + destination : "")
                + ". Zəhmət olmasa qəbul edin və ya imtina edin.";
        notifyWithEmail(
                driverUserId, driverEmail, NotificationType.TRIP_ASSIGNED,
                "Yeni reys gözləyir",
                message,
                "/driver", "Fleetra — yeni reys təhkim olundu", "Reysə bax"
        );
    }

    // Sürücü reysi imtina edəndə bütün aktiv dispetçer/admin istifadəçilərinə
    // — yenidən başqa sürücüyə təhkim etmək üçün dərhal xəbərdar olsunlar
    // (bax DriverTripService#rejectTrip).
    public void notifyTripRejected(Trip trip, String reason) {
        String driverName = trip.getDriver() != null ? trip.getDriver().getFullName() : "Sürücü";
        String message = driverName + " #" + trip.getId() + " nömrəli reysi imtina etdi"
                + (reason != null && !reason.isBlank() ? " (Səbəb: " + reason + ")" : "")
                + ". Yüklər yenidən təhkim edilməyə hazırdır.";
        userRepository.findByRole(Role.DISPATCHER)
                .forEach(u -> notify(u.getId(), NotificationType.TRIP_REJECTED, "Reys imtina edildi", message, "/dispatcher/trips"));
        userRepository.findByRole(Role.ADMIN)
                .forEach(u -> notify(u.getId(), NotificationType.TRIP_REJECTED, "Reys imtina edildi", message, "/admin/trips"));
    }

    // Dispetçer sürücünün cavabını gözləmədən "Qəbul gözlənilir" statusundakı
    // reysi özü ləğv edəndə sürücüyə gedən bildiriş (bax
    // DispatcherService#cancelTrip). Yalnız sürücü artıq bildiriş almışdısa
    // (yəni trip PENDING_ACCEPTANCE-a çatmışdısa) çağırılır.
    public void notifyTripCancelledByDispatcher(Trip trip, Long driverUserId, String driverEmail) {
        if (driverUserId == null) return;
        notifyWithEmail(
                driverUserId, driverEmail, NotificationType.TRIP_CANCELLED,
                "Reys ləğv edildi",
                "#" + trip.getId() + " nömrəli reys dispetçer tərəfindən ləğv edildi, artıq sizə aid deyil.",
                "/driver", "Fleetra — reys ləğv edildi", "Sürücü panelinə bax"
        );
    }

    private static final Map<IncidentType, String> INCIDENT_TYPE_LABEL = Map.of(
            IncidentType.ACCIDENT, "Qəza",
            IncidentType.BREAKDOWN, "Sınma/təmir",
            IncidentType.ROAD_CLOSURE, "Yol bağlanması",
            IncidentType.OTHER, "Digər"
    );

    // Sürücü yolda fövqəladə hal bildirəndə (bax DriverController#reportIncident)
    // bütün aktiv dispetçer/admin istifadəçilərinə TƏCİLİ bildiriş — həm zəng
    // ikonu, həm email, təhlükəsizliklə bağlı olduğu üçün digər "sadəcə
    // zəng ikonu" hadisələrindən fərqli olaraq email də göndərilir ki,
    // tətbiqi açıq saxlamayan dispetçer/admin də dərhal xəbərdar olsun.
    public void notifyIncidentReported(TripIncident incident) {
        String typeLabel = INCIDENT_TYPE_LABEL.getOrDefault(incident.getType(), incident.getType().name());
        String message = (incident.getDriverName() != null ? incident.getDriverName() : "Sürücü")
                + " #" + incident.getTrip().getId() + " nömrəli reysdə fövqəladə hal bildirdi: " + typeLabel
                + (incident.getDescription() != null && !incident.getDescription().isBlank() ? " — " + incident.getDescription() : "");
        userRepository.findByRole(Role.DISPATCHER).forEach(u ->
                notifyWithEmail(u.getId(), u.getEmail(), NotificationType.INCIDENT_REPORTED,
                        "Fövqəladə hal bildirişi", message, "/dispatcher", "Fleetra — fövqəladə hal", "Control Tower-a bax"));
        userRepository.findByRole(Role.ADMIN).forEach(u ->
                notifyWithEmail(u.getId(), u.getEmail(), NotificationType.INCIDENT_REPORTED,
                        "Fövqəladə hal bildirişi", message, "/admin", "Fleetra — fövqəladə hal", "Panelə bax"));
    }

    // Reys öncəsi/sonrası yoxlama siyahısında (DVIR) sürücü ən azı bir
    // maddədə "DEFECT" seçəndə dispetçer/admin-lərə gedən bildiriş — bax
    // DriverController#submitDvir. Təhlükəsizliklə bağlı olduğu üçün
    // (nasaz əyləc/işıq və s.) INCIDENT_REPORTED kimi email də göndərilir.
    public void notifyDvirDefect(DvirInspection inspection) {
        String typeLabel = inspection.getType() == DvirType.PRE_TRIP ? "reys öncəsi" : "reys sonrası";
        String message = (inspection.getDriverName() != null ? inspection.getDriverName() : "Sürücü")
                + " #" + inspection.getTrip().getId() + " nömrəli reysdə " + typeLabel
                + " yoxlamada defekt qeyd etdi"
                + (inspection.getVehiclePlate() != null ? " (" + inspection.getVehiclePlate() + ")" : "")
                + (inspection.getNotes() != null && !inspection.getNotes().isBlank() ? " — " + inspection.getNotes() : "");
        userRepository.findByRole(Role.DISPATCHER).forEach(u ->
                notifyWithEmail(u.getId(), u.getEmail(), NotificationType.DVIR_DEFECT,
                        "Yoxlama siyahısında defekt", message, "/dispatcher", "Fleetra — DVIR defekti", "Control Tower-a bax"));
        userRepository.findByRole(Role.ADMIN).forEach(u ->
                notifyWithEmail(u.getId(), u.getEmail(), NotificationType.DVIR_DEFECT,
                        "Yoxlama siyahısında defekt", message, "/admin", "Fleetra — DVIR defekti", "Panelə bax"));
    }
}
