# Fleetra Android (native Kotlin)

Tam native Android tətbiqi (Kotlin + Jetpack Compose), Expo/React Native əvəzinə. Eyni Spring Boot backend-ə qoşulur. Customer və Driver rolları üçün — Admin/Dispatcher veb paneldə qalır.

## Backend-ə qoşulma

`app/src/main/java/az/fleetra/mobile/config/ApiConfig.kt`:

```kotlin
const val BASE_URL = "http://10.0.2.2:8080/"
```

- **Emulator**: dəyişməyə ehtiyac yoxdur (`10.0.2.2` emulator-dan host-un localhost-una yönləndirir).
- **Fiziki telefon**: bunu komputerinizin LAN IP-sinə dəyişin, məs. `http://192.168.1.72:8080/` (`ipconfig` ilə tapın, Wi-Fi adapter altında). Telefon və komputer eyni Wi-Fi-də olmalı, Windows Firewall-da 8080 portu açıq olmalıdır.

Backend-i işə salmaq: layihənin kök qovluğunda `mvnw.cmd spring-boot:run`.

Cleartext (http://, TLS-siz) trafik `res/xml/network_security_config.xml` ilə icazə verilib — yalnız development üçündür, backend-i həqiqi domain-ə/HTTPS-ə köçürəndə bunu sıxlaşdırın.

## Açmaq və işə salmaq

Android Studio-da bu qovluğu açın (`File → Open` → `LogisticsProject/android`), Gradle sync bitsin, sonra ▶ (Run) düyməsi ilə emulator və ya USB-bağlı telefonda işə salın.

## Struktur

- `network/` — Retrofit API interfeysləri (`AuthApi`, `CustomerApi`, `DriverApi`, `ProfileApi`, `TrackingApi`, `NotificationApi`, `PushApi`, `ChatApi`), `ApiClient` (OkHttp + auth interceptor), DTO-lar backend DTO-ları ilə bir-birinə uyğundur.
- `data/TokenStore.kt` — JWT tokenini DataStore-da saxlayır, sinxron oxuma üçün yaddaşda keşləyir (auth interceptor üçün).
- `messaging/` — Firebase Cloud Messaging: `FleetraFcmService` (mesaj qəbulu + bildiriş göstərmə), `FcmTokenRegistrar` (login/logout-da token qeydiyyatı/silinməsi).
- `ui/auth/` — Login, Qeydiyyat (email təsdiq kodu ilə), Şifrə bərpası (unut/kod/yeni şifrə) + `AuthViewModel`. `MainActivity.kt`-dəki `AuthNavHost` bu axını idarə edir; giriş uğurlu olan kimi `AuthViewModel.session` StateFlow avtomatik Customer/DriverRootScreen-ə keçirir.
- `ui/customer/` — Ana səhifə, Yeni sifariş (GPS-dən pickup koordinatı götürə bilir), Sifarişlərim → Sifariş detalı (marşrut/sürücü/ödəniş + çatdırılmışsa reytinq forması), İzləmə, Söhbət (inbox + sürücü/dispetçer kanalları).
- `ui/driver/` — Aktiv reyslər (yeni reys təklifləri qəbul/imtina, xəritədə naviqasiya, status irəlilətmə, canlı GPS izləmə açar/bağla, çatdırılma şəkli), Tarixçə, Reytinqim (orta bal + rəylər siyahısı), Söhbət (inbox + müştəri/dispetçer kanalları).
- `ui/common/` — Rollar arası paylaşılan: `FleetraTopBar` (zəng ikonu + oxunmamış say), `NotificationListScreen`, `ChatInboxScreen`/`ChatScreen` (polling-based, aşağıda izah olunur), `LoadingView`, `StatusBadge`.
- `ui/profile/` — Bütün rollar üçün ortaq profil ekranı (ad, istifadəçi adı, şifrə dəyişmə; Customer/Driver üçün əlavə foto/doğum tarixi/millət/yer).
- `location/LocationTrackingService.kt` — foreground GPS servisi, aktiv reys seçiləndə arxa planda məkanı backend-ə göndərir.

## Söhbət (chat) — necə işləyir

Backend-də canlı mesaj göndərişi üçün STOMP `@MessageMapping` YOXDUR — göndərmə tam REST-dir (`POST /api/chat/cargo/{cargoId}/messages`), STOMP yalnız server→client push üçündür (veb tərəfdə). Bu səbəbdən mobil tərəf WebSocket/STOMP client əlavə etmək əvəzinə sadə polling istifadə edir (`ChatScreen.kt` — hər 4 saniyədə tam tarixçəni yenidən yükləyir, backend-də pagination olmadığı üçün bu tam məntiqlidir). Uzun müddət ekranda qalmayan söhbətlərdə əlavə şəbəkə yükü yaratmır (polling loop yalnız `ChatScreen` kompozisiyada olduğu müddətcə işləyir).

## Bilinən məhdudiyyətlər

- Bu mühitdə (sandbox) Gradle/Android SDK-ya giriş olmadığı üçün tam build test edilə bilmədi — bütün fayllar diqqətlə əl ilə yoxlanıldı (paket/qovluq uyğunluğu, mötərizə balansı, import-lar, DTO sahə adlarının backend ilə tam uyğunluğu), amma **Android Studio-da ilk sync/build sizin özünüzdə olacaq**. Əgər bir asılılıq versiyası tapılmasa, Android Studio adətən "quick fix" təklif edir (versiyanı ən uyğununa dəyişmək) — bunu qəbul etməyiniz kifayətdir.
- **Firebase push bildirişləri** — kod hazırdır (`messaging/FleetraFcmService.kt`, `messaging/FcmTokenRegistrar.kt`), amma İŞLƏMƏSİ ÜÇÜN İKİ ŞEY LAZIMDIR:
  1. **Öz Firebase layihənizi yaradın** (console.firebase.google.com), Android tətbiqi əlavə edin (paket adı: `az.fleetra.mobile`), yüklədiyiniz `google-services.json` faylını `android/app/` qovluğuna qoyun. Bu fayl yoxdursa tətbiq normal build olunur və işləyir, sadəcə push passiv qalır (bax `app/build.gradle.kts`-dəki şərti `google-services` plugin tətbiqi).
  2. **Backend tərəfdə real göndəriş hələ qoşulmayıb** — hazırda backend yalnız FCM tokenləri qeydə alır (`FcmToken` cədvəli, `POST /api/push/fcm-subscribe`), lakin faktiki mesaj göndərməsi üçün Firebase Admin SDK (`com.google.firebase:firebase-admin` Maven asılılığı) və Firebase layihənizin xidmət hesabı (service account) JSON açarı backend-ə əlavə edilməlidir (bax `PushNotificationService.java`-dəki şərh). Bu, backend tərəfdə ayrıca görüləcək iş.
  3. Bildiriş göstərmək üçün Android 13+ üzərində `POST_NOTIFICATIONS` icazəsinin runtime təsdiqi (əl ilə istifadəçidən soruşulması) hələ tətbiq edilməyib — icazə manifestdə bəyan olunub, lakin sistem dialoqu ilk açılışda göstərilmir (istifadəçi Ayarlar-dan əl ilə aça bilər).
- `NewOrderScreen`-də anbar seçimi yoxdur, sadəcə ünvan mətni.
- Sifariş detalında beynəlxalq daşıma (gömrük/sənəd/sərhəd) məlumatları hələ göstərilmir — veb tərəfdə mövcuddur, mobil Faza 3-ə saxlanılıb.
- Ödəniş — hazırda yalnız veb tərəfdə (Stripe). Mobil tətbiqdə kart ödənişi Faza 3-də planlaşdırılıb (istifadəçi tərəfindən təsdiqləndi: tam Stripe kart inteqrasiyası, sadəcə faktura görünüşü deyil).
