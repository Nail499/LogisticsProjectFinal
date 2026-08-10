# Fleetra-nı DigitalOcean App Platform-a yerləşdirmək

Bu sənəd `.do/app.yaml`-da hazırlanmış tətbiq spesifikasiyasını canlıya çıxarmaq üçün addım-addım təlimatdır.

## 1. Ön şərtlər

- GitHub-da `Nail499/LogisticsProjectFinal` repo-su ictimai/DigitalOcean-a bağlana bilən olmalıdır.
- DigitalOcean hesabı və (istəyə görə) `doctl` CLI.
- Aşağıdakı sirlərin hazır olması (heç biri repoda saxlanılmır):

| Dəyişən | Haradan alınır |
|---|---|
| `JWT_SECRET` | İstənilən uzun təsadüfi sətir (məs. `openssl rand -base64 48`) |
| `ADMIN_INITIAL_PASSWORD` | Özün seç — güclü şifrə (bax aşağıda "Default admin" bölməsi) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail ünvanı + [App Password](https://myaccount.google.com/apppasswords) |
| `STRIPE_PUBLISHABLE_KEY` / `STRIPE_SECRET_KEY` | Stripe Dashboard → Developers → API keys |
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | `npx web-push generate-vapid-keys` |
| `GROQ_API_KEY` | console.groq.com (boş buraxıla bilər — AI chat sadəcə deaktiv olar) |

## 2. Xərc — hansı planı seçim?

DigitalOcean-da statik sayt (frontend) pulsuzdur, amma backend (service) və verilənlər bazası artıq pulsuz deyil:

| Komponent | Tövsiyə olunan plan | RAM | Qiymət |
|---|---|---|---|
| backend (service) | `apps-s-1vcpu-1gb-fixed` — bu `.do/app.yaml`-da artıq seçilib | 1 GB | ~10 $/ay |
| db (Postgres, Dev Database) | `db-s-dev-database` | 512 MB | ~7 $/ay |
| frontend (static site) | avtomatik, plan seçimi yoxdur | — | pulsuz |

**Cəmi: ~17 $/ay.** Daha ucuz istəsən, backend üçün `apps-s-1vcpu-0.5gb` (512 MB, ~5 $/ay) seçə bilərsən, amma Spring Boot + Hibernate + Security + WebSocket birlikdə bəzən 512 MB-a sığmayıb yaddaş xətası (OOM) verə bilər — ilk növbədə 1 GB ilə başlamağı məsləhət görürəm, sonra lazım olsa aşağı sala bilərsən.

## 3. Tətbiqi addım-addım yarat

DigitalOcean artıq tətbiq **yaradılarkən** birbaşa YAML yapışdırmağa icazə vermir — spesifikasiyanı yalnız tətbiq artıq mövcud olandan sonra redaktə etmək olur. Ona görə əvvəlcə sadə (natamam) tətbiq yaradıb, sonra tam spesifikasiya ilə əvəz edəcəyik:

1. [cloud.digitalocean.com/apps](https://cloud.digitalocean.com/apps) → **Create App**.
2. **Service Provider**: GitHub seç, hesabını bağla (ilk dəfədirsə icazə istəyəcək), repo olaraq `Nail499/LogisticsProjectFinal`, branch `master` seç → **Next**.
3. DigitalOcean avtomatik resurs siyahısı təklif edəcək (çox güman Dockerfile-ı tapıb backend-i tək "Web Service" kimi tanıyacaq, frontend/db-ni görməyəcək) — narahat olma, bunu olduğu kimi qəbul edib sonuna qədər sehrbazı keç, ad ver (məs. `fleetra`), region seç (istifadəçilərinə ən yaxın, məs. Frankfurt) → **Create Resources**. İlk (natamam) deploy başlayacaq.
4. Deploy bitəndə (və ya davam edərkən) tətbiqin **Overview** səhifəsinə keç → yuxarıda **Settings** tabı → aşağı sürüş, **App Spec** bölməsini tap → **Edit**.
5. Açılan redaktordakı mətni tamamilə sil, bu repodakı `.do/app.yaml` faylının içindəkini köçürüb yapışdır → **Save**.
6. DigitalOcean indi tam spesifikasiyanı tanıyacaq: **backend** (Docker service), **frontend** (Static Site), **db** (Dev Database, Postgres 16), üstəlik `ingress` bölməsi ilə path marşrutlanması (`/api`, `/ws`, `/uploads`, `/swagger-ui`, `/v3/api-docs`, `/actuator` → backend, qalan hər şey → frontend — çünki DO hər komponentə ayrıca subdomain vermir, hamısı EYNİ domendə paylaşılır).
7. Bu addımda SECRET işarəli dəyişənlər (yuxarıdakı 1-ci bölmədəki cədvəl — `JWT_SECRET`, `ADMIN_INITIAL_PASSWORD`, `MAIL_USERNAME`, və s.) boş sətir kimi görünəcək — hər birinin qarşısına real dəyəri yaz. `SPRING_DATASOURCE_*`, `VITE_API_URL`, `FRONTEND_URL` kimi dəyişənlər artıq avtomatik doldurulub, onlara toxunma.
8. **Save**/**Deploy** — yeni spesifikasiya ilə yenidən deploy başlayır (adətən 3-6 dəqiqə). Bitəndə "Deployed" statusu görünəcək, backend və frontend eyni ünvanda (`https://fleetra-xxxxx.ondigitalocean.app`) əlçatan olacaq.

## 4. Deploydan sonra yoxla

Frontend və backend eyni ünvandadır (məs. `https://fleetra-xxxxx.ondigitalocean.app`):

- Sağlamlıq: `.../actuator/health` → `{"status":"UP"}`.
- API sənədləri: `.../swagger-ui.html`.
- Ana səhifə: `.../` açılmalı və backend-ə qoşulmalıdır (Login səhifəsi görünürsə, hər şey düzgündür).
- `admin` / (sənin təyin etdiyin `ADMIN_INITIAL_PASSWORD`) ilə giriş et.

## 5. Bilinən məhdudiyyət — yüklənən fayllar

`file.upload-dir=uploads` (profil şəkilləri, çatdırılma sübutu fotoları, gömrük sənədləri, chat şəkilləri) hazırda **yerli diskə** yazılır. App Platform-un konteyner disk sahəsi **ephemeral**-dir — hər yeni deploy, restart və ya miqyaslanma zamanı silinir. Yəni:

- Bu, funksional baxımdan indi işləyəcək (fayl yüklə, həmin sessiyada gör).
- Amma növbəti deploydan sonra əvvəlki yüklənmiş fayllar İTİR.

Bunun düzgün həlli — DigitalOcean Spaces (S3-uyğun obyekt yaddaşı) istifadə etmək və `FileStorageService`-i yerli disk əvəzinə Spaces-ə yazacaq şəkildə dəyişməkdir. Bu, ayrıca (test edilməli) bir iş həcmidir və bu sessiyada edilməyib — hazırkı deploy funksional işləyəcək, sadəcə bu məhdudiyyəti bilərək.

## 6. Default admin haqqında

`DataInitializer` ilk açılışda `admin` istifadəçisi yoxdursa yaradır. Şifrə `ADMIN_INITIAL_PASSWORD` env dəyişənindən götürülür — bu təyin olunmasa, defolt olaraq `admin123` istifadə olunur (yalnız lokal inkişaf üçün nəzərdə tutulub). **Production-da bu dəyişəni mütləq güclü bir şifrə ilə təyin et.**
