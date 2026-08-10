package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.push.PushSubscribeRequest;
import com.ltc.logisticsproject.entity.PushSubscription;
import com.ltc.logisticsproject.repository.FcmTokenRepository;
import com.ltc.logisticsproject.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.List;

// Brauzer push bildirişləri — Web Push protokolu + VAPID (bax
// entity/PushSubscription, PushSubscriptionController). NotificationService
// hər bildiriş yaradanda (notify()) bu servisi də çağırır, ona görə
// mövcud bütün 6 tetiklənmə nöqtəsi (xoş gəlmisiniz, sifariş, yük
// götürüldü/çatdırıldı, şifrə dəyişdi, ödəniş qəbul edildi) əlavə koda
// ehtiyac olmadan avtomatik push da göndərir.
//
// Qeyd: JSON payload burada Jackson ilə yox, əl ilə qurulur — bu layihə
// Spring Boot 4-dədir və spring-boot-starter-webmvc daxili Jackson-u
// "tools.jackson.core" (Jackson 3) qrupu altında gətirir, "com.fasterxml.
// jackson.databind" isə yalnız jjwt-jackson-un keçici (runtime-scope)
// asılılığı kimi mövcuddur — compile zamanı görünmür (bax
// `mvn dependency:tree` nəticəsi). Payload cəmi 3 sadə mətn sahəsi
// olduğu üçün əl ilə JSON qurmaq əlavə asılılıqdan daha etibarlıdır.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushNotificationService {

    final PushSubscriptionRepository pushSubscriptionRepository;

    // Mobil (Android) FCM token-ləri bax entity/FcmToken.java —
    // PushSubscriptionController#registerFcmToken vasitəsilə yığılır. HAZIRDA
    // BURADA İSTİFADƏ OLUNMUR: real FCM göndərişi Firebase Admin SDK
    // (com.google.firebase:firebase-admin) asılılığı VƏ Firebase layihəsinin
    // xidmət hesabı (service account) JSON açarı tələb edir — bu açar
    // repoya/env-ə hələ əlavə olunmayıb. Bu sahə yalnız tokenlərin artıq
    // yığıldığını və gələcəkdə sendPush()-un içində FirebaseMessaging.
    // getInstance().sendMulticast(...) çağırışı üçün hazır olduğunu
    // sənədləşdirmək məqsədilə saxlanılıb.
    final FcmTokenRepository fcmTokenRepository;

    @Value("${vapid.public-key}")
    String publicKey;

    @Value("${vapid.private-key}")
    String privateKey;

    @Value("${vapid.subject}")
    String subject;

    PushService pushService;

    @PostConstruct
    public void init() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        this.pushService = new PushService(publicKey, privateKey, subject);
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void subscribe(Long userId, PushSubscribeRequest request) {
        if (request.getEndpoint() == null || request.getKeys() == null) return;

        PushSubscription sub = pushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .orElse(PushSubscription.builder().endpoint(request.getEndpoint()).build());
        sub.setUserId(userId);
        sub.setP256dh(request.getKeys().getP256dh());
        sub.setAuth(request.getKeys().getAuth());
        pushSubscriptionRepository.save(sub);
    }

    public void unsubscribe(String endpoint) {
        if (endpoint == null) return;
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
    }

    // Bax NotificationService#notify — hər yeni in-app bildirişdə çağırılır.
    // Bir istifadəçinin bir neçə cihazdan abunəliyi ola bilər, hər birinə
    // ayrıca göndərilir; bir abunəliyin uğursuz olması digərlərini
    // dayandırmır (try/catch hər dövr addımında).
    public void sendPush(Long userId, String title, String message, String link, String frontendUrl) {
        if (userId == null || pushService == null) return;
        List<PushSubscription> subs = pushSubscriptionRepository.findByUserId(userId);
        if (subs.isEmpty()) return;

        String url = link != null ? frontendUrl + link : frontendUrl;
        String payload = "{\"title\":\"" + escapeJson(title) + "\","
                + "\"body\":\"" + escapeJson(message) + "\","
                + "\"url\":\"" + escapeJson(url) + "\"}";

        for (PushSubscription sub : subs) {
            try {
                Notification notification = new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload);
                // Qaytarılan HttpResponse (org.apache.http.HttpResponse) qəsdən
                // tutulmur/tipləşdirilmir — bu tip web-push-un öz POM-unda
                // yalnız runtime-scope-dur (bax dependency:tree), compile
                // zamanı görünmür. Statusu yoxlamaq (404/410-da köhnəlmiş
                // abunəliyi silmək) mümkün olardı, lakin bunun üçün əlavə
                // compile-scope asılılıq lazımdır — sadəlik naminə saxlanmır.
                pushService.send(notification);
            } catch (Exception e) {
                // Push göndərilməsi uğursuz olsa da əsas bildiriş axını
                // (in-app + email) artıq tamamlanıb — burada dayandırmırıq
                // (eyni ehtiyat NotificationService#notifyWithEmail-də var).
            }
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
