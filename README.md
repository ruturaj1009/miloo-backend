# Miloo - Next-Gen Real-Time Interaction & Dating Backend (Spring Boot Modular Monolith)

Production-grade modular monolith backend for the **Miloo** cross-platform application, engineered using **Spring Boot 3 + Maven** and **PostgreSQL (PostGIS + pgvector)**.

---

## 1. Architectural Highlights

* **Build Tool:** Apache Maven (`pom.xml`)
* **Framework:** Spring Boot 3.3.4, Java 17/21 LTS
* **Database Topology:** 5 decoupled logical schemas guaranteeing zero database redesign when transitioning to microservices (Go / Python) in Phase 2:
  * `auth_ctx`: Account records, phone/email auth, password hashing, and JWT issuance.
  * `people_ctx`: User profiles, PostGIS spatial geography (`Point, 4326`), discovery candidate search, and Cloudflare R2 presigned media metadata.
  * `interaction_ctx`: Swipes (`LIKE`, `PASS`, `SUPERLIKE`) and reciprocal atomic matching engine.
  * `chat_ctx`: Historical chat messages and WebRTC audio/video call session records.
  * `ai_ctx`: User vector embeddings (`pgvector`) and interest similarity matching.
* **Real-Time Layer:** STOMP over WebSocket broker (`/ws/signaling`) for live chat messaging, instant double-opt-in match alerts, typing indicators, and WebRTC SDP/ICE signaling.
* **Storage Layer:** Cloudflare R2 / AWS S3 SDK v2 presigned upload URLs with zero-config local development mock fallback.

---

## 2. Prerequisites & Quick Start

### Prerequisites
1. **Java Development Kit (JDK):** Version 17 or 21 LTS installed.
2. **Apache Maven:** `mvn -version`
3. **PostgreSQL 15+:** With `postgis`, `uuid-ossp`, and optionally `vector` extensions installed (e.g. Supabase, Docker, or native PostgreSQL).

### Setup PostgreSQL Database
Run in your local PostgreSQL or Supabase SQL console:
```sql
CREATE DATABASE miloo_db;
```

### Configure Environment Variables (Optional)
Defaults in `src/main/resources/application.yml` connect to `localhost:5432/miloo_db` with credentials `postgres / postgres`. You can override them via environment variables:
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/miloo_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your_password
export JWT_SECRET=your_super_secret_base64_256bit_key
```

### Build & Run with Maven
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Run Spring Boot application
mvn spring-boot:run
```

The server starts on `http://localhost:8080` and applies database migrations (`V1` to `V7`) automatically via Flyway.

---

## 3. Core API Endpoints

### Auth (`auth_ctx`)
* `POST /api/v1/auth/send-otp` - Dispatch 6-digit OTP with 5-min TTL via Twilio, MSG91, or SMTP.
* `POST /api/v1/auth/login` - Request OTP login challenge (same dispatch mechanism).
* `POST /api/v1/auth/verify-otp` - Verify 6-digit OTP code against PostgreSQL (master demo OTP: `123456`).
* `POST /api/v1/auth/register` - Create new account & get JWT session.
* `GET /api/v1/auth/me` - Get current authenticated account details.
* *Detailed test cases & curl commands:* See [AUTH_TESTING_GUIDE.md](docs/AUTH_TESTING_GUIDE.md).

### People & Discovery (`people_ctx`)
* `GET /api/v1/profiles/me` - Get current user's profile and photos.
* `POST /api/v1/profiles/me` - Upsert profile information & GPS coordinates.
* `GET /api/v1/profiles/{userId}` - View candidate public profile with calculated distance.
* `GET /api/v1/media/upload-url` - Generate Cloudflare R2 presigned PUT upload URL.
* `POST /api/v1/profiles/me/photos` - Register uploaded photo metadata.
* `DELETE /api/v1/profiles/me/photos/{mediaId}` - Remove photo.
* `GET /api/v1/discovery/feed` - PostGIS geospatial candidate feed within radius.

### Interaction & Matching (`interaction_ctx`)
* `POST /api/v1/swipes` - Record `LIKE`, `PASS`, or `SUPERLIKE`. Reciprocal likes trigger instant match creation and STOMP push notifications.
* `GET /api/v1/matches` - List all active mutual matches with partner profiles.
* `DELETE /api/v1/matches/{matchId}` - Unmatch a user.

### Chat & Calling (`chat_ctx`)
* `GET /api/v1/chat/conversations` - Active conversations with latest message & unread counter.
* `GET /api/v1/chat/{matchId}/history` - Paginated chat history.
* `POST /api/v1/chat/{matchId}/messages` - REST fallback to send a message.
* `PUT /api/v1/chat/{matchId}/read` - Mark conversation messages as read.

### STOMP WebSocket Broker (`/ws/signaling`)
* **Client Handshake:** `ws://localhost:8080/ws/signaling` (pass `Authorization: Bearer <token>` in STOMP `CONNECT` header).
* **Subscriptions:**
  * `/user/queue/messages` : Live incoming chat messages.
  * `/user/queue/matches` : Real-time "It's a Match!" push notification.
  * `/user/queue/typing` : Live typing indicators.
  * `/user/queue/signaling` : WebRTC SDP Offers, Answers, and ICE candidates.
* **Publish Destinations:**
  * `/app/chat.send` : Send chat message.
  * `/app/chat.typing` : Send typing state.
  * `/app/call.signal` : Send WebRTC SDP/ICE payload.
  * `/app/call.action` : Invite, Accept, Reject, End call.

---

## 4. Connecting the React Native Frontend

1. Ensure the backend is running (`mvn spring-boot:run`).
2. In `frontend/src/api/client.ts`, `USE_MOCK_API` is set to `false`.
3. If running on Android Emulator, the app connects automatically to `http://10.0.2.2:8080`.
4. If running on physical devices on the same Wi-Fi, set:
   ```bash
   EXPO_PUBLIC_API_URL=http://<YOUR_LOCAL_IP>:8080/api/v1
   EXPO_PUBLIC_WS_URL=ws://<YOUR_LOCAL_IP>:8080/ws/signaling
   ```
5. Pre-seeded demo account:
   * Phone: `+15551234567` (or email: `alex@example.com`)
   * OTP: `123456`
