# 📱 MessenGO — Human-Centric Messaging Application

> A modern Android messaging application built with a microservices architecture, featuring real-time communication, WebRTC, and secure cloud backup.

---

## 🏗️ Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                    ANDROID CLIENT (Kotlin)                    │
│  Jetpack Compose · Room DB · Hilt DI · WorkManager (Outbox)  │
│  WebRTC Native · EncryptedSharedPrefs                        │
└────────────────────────┬─────────────────────────────────────┘
                         │  HTTPS / WSS
┌────────────────────────▼─────────────────────────────────────┐
│              NGINX API GATEWAY (Reverse Proxy)               │
│              Port 80 — Route-based dispatching               │
└──┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬───┘
   │      │      │      │      │      │      │      │      │
┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐
│Auth ││User ││Chat ││Msg  ││Media││Call ││Util ││Srch ││Notif│
│3001 ││3002 ││3003 ││3004 ││3005 ││3006 ││3007 ││3008 ││3009 │
└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘
   │      │      │      │      │      │      │      │      │
┌──▼──────▼──────▼──────▼──────▼──────▼──────▼──────▼──────▼───┐
│  MongoDB 7.0 (Database-per-Service)  ·  Redis 7 (Streams)   │
│  MinIO S3 (Media & Backup)  ·  Coturn (STUN/TURN)           │
└──────────────────────────────────────────────────────────────┘
```

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Android** | Kotlin · Jetpack Compose · Room · Hilt · Retrofit · OkHttp · WorkManager |
| **Backend** | Node.js 20 · Express.js · Mongoose · Socket.IO / ws |
| **Database** | MongoDB 7.0 (7 databases) · Redis 7 (Streams, Cache, Pub/Sub) |
| **Storage** | MinIO S3 (Pre-signed URL upload) |
| **Realtime** | WebSocket (Messaging) · WebRTC + Coturn (Audio/Video Call) |
| **Security** | JWT (Access 15m + Refresh 30d) · AES-256 (Backup) · OTP (SMS/Mock) |
| **DevOps** | Docker Compose · GitHub Actions CI/CD · Nginx Gateway |

## 📦 Project Structure

```
MessenGO/
├── android/                 # Android app (Kotlin + Jetpack Compose)
├── backend/                 # Node.js Microservices
│   ├── services/
│   │   ├── auth-service/    # OTP, JWT, Refresh Token
│   │   ├── user-service/    # Profile, Device, Contact
│   │   ├── chat-service/    # Conversation CRUD
│   │   ├── message-service/ # WebSocket, Redis Streams
│   │   ├── media-service/   # MinIO Pre-signed URL
│   │   ├── call-service/    # WebRTC Signaling
│   │   ├── utility-service/ # Backup AES-256 + ZIP
│   │   └── search-service/  # Full-text message search
│   └── shared/              # JWT verify, Redis client, middleware
├── docker/                  # Docker configs (Nginx, MongoDB init, Coturn)
├── Diagrams/                # Architecture & Sequence Diagrams (Frozen)
├── Docs/                    # SRS, User Story, Proposal (Frozen)
├── docker-compose.yml       # Infrastructure services
├── .env.example             # Environment template
└── .github/workflows/       # CI/CD pipeline
```

## 🚀 Installation & Running Guide

### Requirements
- Docker Desktop (v4.x+)
- Node.js 20 LTS
- Android Studio Hedgehog+ (JDK 17)
- Git

### Step 1: Clone repo

```bash
git clone https://github.com/MessenGOTeam/MessenGO.git
cd MessenGO
```

### Step 2: Create .env file

```bash
cp .env.example .env
# Edit .env if you need to change ports or credentials
```

### Step 3: Start Infrastructure

```bash
docker compose up -d
```

Check status:
```bash
docker compose ps
```

Services will run at:
| Service | URL |
|---------|-----|
| API Gateway | http://localhost:80 |
| MongoDB | localhost:27017 |
| Redis | localhost:6379 |
| MinIO Console | http://localhost:9001 |
| MailHog (Mock OTP) | http://localhost:8025 |

### Step 4: Run Backend (example Auth Service)

```bash
cd backend/services/auth-service
npm install
npm run dev
```

### Step 5: Open Android Studio

```
File → Open → select android/ folder
Sync Gradle → Run on Emulator or Device
```

## 🌿 Git Flow

```
main          ← Production (only merge from dev after testing)
  └── dev     ← Development branch
       ├── feature/backend-auth-service
       ├── feature/android-auth-ui
       └── feature/...
```

**Rules:**
- ❌ DO NOT commit directly to `main` or `dev`
- ✅ Create `feature/*` branch from `dev`
- ✅ Pull Request + 1 Approval before merge
- ✅ Document any design changes in `CHANGELOG.md`

## 👥 Team Members

| MSSV | Name | Role |
|---|---|---|
| **524H0032** | **Trương Minh Thành** | Backend & Android & Cloud Deployment |
| **524H0023** | **Huỳnh Trần Thiên Phú** | Backend & Android & UI/UX |

## 📄 License

[MIT License](LICENSE)
