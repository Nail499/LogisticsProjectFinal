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

## 2. Tətbiqi yarat

1. DigitalOcean Dashboard → **Apps** → **Create App**.
2. GitHub repo-nu seç (`Nail499/LogisticsProjectFinal`, branch `master`).
3. "Edit Your App Spec" düyməsinə keç, bu repodakı `.do/app.yaml` faylının məzmununu yapışdır (və ya `doctl apps create --spec .do/app.yaml` işlət).
4. App Platform 3 komponenti tanıyacaq: **backend** (Docker), **frontend** (statik sayt), **db** (Postgres, dev tier — pulsuzdur, kiçikdir).
5. SECRET işarəli hər dəyişən üçün (yuxarıdakı cədvəl) UI səni real dəyəri daxil etməyə çağıracaq.
6. **Create Resources** — ilk deploy başlayır (Docker image build olunur, bir neçə dəqiqə çəkə bilər).

## 3. Deploydan sonra yoxla

- Backend sağlamlıq: `https://<backend-domeni>/actuator/health` → `{"status":"UP"}`.
- API sənədləri: `https://<backend-domeni>/swagger-ui.html`.
- Frontend: `https://<frontend-domeni>/` açılmalı və backend-ə qoşulmalıdır (Login səhifəsi görünürsə, CORS/`VITE_API_URL` düzgündür).
- `admin` / (sənin təyin etdiyin `ADMIN_INITIAL_PASSWORD`) ilə giriş et.

## 4. Bilinən məhdudiyyət — yüklənən fayllar

`file.upload-dir=uploads` (profil şəkilləri, çatdırılma sübutu fotoları, gömrük sənədləri, chat şəkilləri) hazırda **yerli diskə** yazılır. App Platform-un konteyner disk sahəsi **ephemeral**-dir — hər yeni deploy, restart və ya miqyaslanma zamanı silinir. Yəni:

- Bu, funksional baxımdan indi işləyəcək (fayl yüklə, həmin sessiyada gör).
- Amma növbəti deploydan sonra əvvəlki yüklənmiş fayllar İTİR.

Bunun düzgün həlli — DigitalOcean Spaces (S3-uyğun obyekt yaddaşı) istifadə etmək və `FileStorageService`-i yerli disk əvəzinə Spaces-ə yazacaq şəkildə dəyişməkdir. Bu, ayrıca (test edilməli) bir iş həcmidir və bu sessiyada edilməyib — hazırkı deploy funksional işləyəcək, sadəcə bu məhdudiyyəti bilərək.

## 5. Default admin haqqında

`DataInitializer` ilk açılışda `admin` istifadəçisi yoxdursa yaradır. Şifrə `ADMIN_INITIAL_PASSWORD` env dəyişənindən götürülür — bu təyin olunmasa, defolt olaraq `admin123` istifadə olunur (yalnız lokal inkişaf üçün nəzərdə tutulub). **Production-da bu dəyişəni mütləq güclü bir şifrə ilə təyin et.**
