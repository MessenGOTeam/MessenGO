# 📋 CHANGELOG — MessenGO

All significant changes in the project will be recorded here.  
Follow the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.

---

## [Unreleased]

### 🏗️ Architecture Decision Records (ADR)

> From **Design Freeze (2026-10-17)**, any design changes from the original documents must be recorded here.

| ADR # | Date | Decision | Reason | Impact |
|-------|------|----------|--------|--------|
| — | — | _(No changes yet)_ | — | — |

---

## [0.2.0] — 2026-10-17

### 🔒 Design Freeze
- Freezed all design documents: SRS, User Story, ERD, Architecture Diagram, 6 Sequence Diagrams
- All changes from this point must be recorded in the ADR section above

### Added
- README.md with complete setup guide
- CHANGELOG.md for Architecture Decision Records
- Notification Service route trong Nginx Gateway

### Fixed
- Removed `.env` from Git tracking (security credentials)

---

## [0.1.0] — 2026-10-15

### Added
- Initialized repo with MIT License
- Documents: SRS & User Story, Proposal, Use Case Diagram
- Architecture Diagram: 9 Microservices, 3-tier architecture
- 6 Sequence Diagrams: Auth, Contact, Messaging, Media, WebRTC, Sync/Backup
- Docker Compose: MongoDB 7.0, Redis 7, MinIO, Coturn, MailHog, Nginx Gateway
- MongoDB init script: 7 databases, 24+ indexes (Database-per-Service)
- Nginx reverse proxy: 10 routes (HTTP + WebSocket)
- CI/CD pipeline: GitHub Actions (validate infra, backend lint, android test)
- Environment configuration: `.env` + `.env.example`
