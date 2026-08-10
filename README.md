# Fleetra — Logistics & Fleet Management Platform

![CI](https://github.com/Nail499/LogisticsProjectFinal/actions/workflows/ci.yml/badge.svg)

Fleetra is a full-stack logistics/dispatch platform covering the whole cargo lifecycle — from customer booking to live GPS tracking, driver hours-of-service, customs clearance and invoicing. It ships as three coordinated apps: a Spring Boot backend, a React web app, and a native Android driver/customer app.

## Tech stack

| Layer | Stack |
|---|---|
| Backend | Java 17, Spring Boot 4, Spring Security (JWT), Spring Data JPA, PostgreSQL, WebSocket (STOMP), Springdoc/OpenAPI |
| Frontend | React 18, Vite, Tailwind CSS, react-router, react-leaflet, Stripe.js, Recharts, i18next (AZ/RU/EN) |
| Mobile | Kotlin, Jetpack Compose, Retrofit, DataStore, Firebase Cloud Messaging |
| Integrations | Stripe (payments), Web Push/VAPID, Firebase FCM, Groq (AI support chat), SMTP (email verification) |
| CI | GitHub Actions — backend compile + frontend build on every push |

## Roles & core features

- **Customer** — place/track cargo, live map tracking, in-app chat with driver/dispatcher, rate completed trips, view invoices, customs cost calculator, AI support chat.
- **Driver** — accept/reject trips, turn-by-turn trip lifecycle (pickup → in transit → delivered), proof-of-delivery photo upload, expense receipts, DVIR pre/post-trip inspection, hours-of-service logging, earnings view, border-crossing logging for international trips.
- **Dispatcher** — Control Tower live map, KPI dashboard, auto-assignment suggestions, trailer pool management, route optimization for multi-stop trips, offline payment recording, customs/trade document handling.
- **Admin** — user & vehicle/trailer management, tariff configuration, audit log, CSV/Excel report export, payment/invoice oversight.

Shared across roles: JWT authentication, real-time WebSocket location broadcast, push notifications (browser + Firebase), dark mode, and full AZ/RU/EN localization.

## Project structure

```
LogisticsProject/
├── src/main/java/com/ltc/logisticsproject/   # Spring Boot backend
│   ├── controller/   # REST endpoints, grouped by role
│   ├── service/       # business logic
│   ├── entity/         # JPA entities
│   ├── dto/             # request/response DTOs, split into domain sub-packages
│   │   ├── auth/ chat/ customs/ payment/ profile/ push/ rating/
│   ├── repository/  # Spring Data repositories
│   ├── security/      # JWT filter, UserDetailsService
│   └── config/         # Security, CORS, WebSocket, OpenAPI config
├── frontend/                                     # React web app (Vite)
│   └── src/{pages,components,layouts,api,context,i18n}/
├── android/                                     # Native Android app (Kotlin/Compose)
└── .github/workflows/ci.yml           # CI pipeline
```

## Getting started

### Backend

Requires Java 17 and a local PostgreSQL database. Secrets are externalized via environment variables (never committed) — set at minimum `DB_PASSWORD` and `JWT_SECRET` before running:

```bash
./mvnw spring-boot:run
```

The API is documented via Swagger UI once the server is running: `http://localhost:8080/swagger-ui.html`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Android

Open the `android/` folder in Android Studio. See `android/README.md` for device setup (emulator vs. physical device, Firebase configuration).

## API documentation

Every backend endpoint is auto-documented via [springdoc-openapi](https://springdoc.org/):

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
