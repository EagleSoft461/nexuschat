# 🗺️ NexusChat — Roadmap

This document tracks the development progress and future plans for NexusChat.

---

## ✅ Sprint 1 — Core Infrastructure (Completed)

- [x] Spring Boot 3 + Java 17 project setup
- [x] PostgreSQL entities: `User`, `Room`, `Message`, `RoomMember`
- [x] JWT authentication (register, login)
- [x] JWT validation on WebSocket CONNECT frame
- [x] WebSocket config (STOMP + SockJS)
- [x] Redis Pub/Sub integration
- [x] End-to-end message flow (WebSocket → DB → Redis → broadcast)
- [x] Docker Compose (app + postgres + redis)
- [x] Multi-stage Dockerfile with non-root user

---

## ✅ Sprint 2 — Presence & Polish (Completed)

- [x] Presence system — online/offline tracking via Redis
- [x] Typing indicator — broadcast `{username} is typing...` via WebSocket
- [x] `WebSocketAuthInterceptor` — validate JWT on CONNECT, reject invalid tokens
- [x] Error handling — global exception handler (`@ControllerAdvice`)
- [x] Input validation responses — proper 400 responses with field errors
- [x] Fix `open-in-view` warning in JPA config
- [x] UTF-8 encoding fix for Turkish characters
- [x] Multi-language UI (TR/EN)
- [x] Room deletion (owner only)
- [x] Swagger / OpenAPI documentation

---

## ✅ Sprint 3 — Advanced Features (Completed) — v1.0.0

- [x] Typing indicator — broadcast `{username} is typing...` via presence channel
- [x] Read receipts — track which messages a user has seen (`lastReadMessageId` per member)
- [x] Message editing — `PATCH /api/messages/{id}`, sets `edited: true`, broadcasts via Redis
- [x] File/image attachment support — `fileUrl` + `fileName` fields on `Message`, sent via WebSocket
- [x] Private rooms — invite-only via `POST /api/rooms/{id}/invite` (OWNER/ADMIN only)
- [x] Direct messages (DM) — `POST /api/rooms/dm`, idempotent (returns existing DM if present)
- [x] Unread message count — per-room `GET /api/messages/room/{id}/unread` and bulk `GET /api/messages/unread`
- [x] UI — unread badges on room list
- [x] UI — edit/delete buttons on own messages (hover)
- [x] UI — file attachment picker with preview
- [x] UI — DM start panel
- [x] UI — room type selector (PUBLIC / PRIVATE) on create
- [x] UI — read receipt tick marks (✓✓) on messages
- [x] UI — room list merges public + user rooms (deduplication fix)
- [x] Bug fixes — LazyInitializationException, docker-compose logging indent, JWT charset, SCAN vs KEYS

---

## ✅ Sprint 4 — Scaling & Production (Completed) — v4.0.0

- [x] RabbitMQ STOMP broker relay — replaces in-memory broker, full message broker with fallback
- [x] Rate limiting — Redis token bucket (Bucket4j), per-user/IP, 429 with retry headers
- [x] Refresh token support — rotation on each use, revoke all on logout
- [x] Kubernetes manifests — Deployment, Service, ConfigMap, Secret, Ingress, HPA, PVC
- [x] Health check endpoint — `/actuator/health` via Spring Boot Actuator
- [x] Metrics with Micrometer + Prometheus — `/actuator/prometheus`, K8s pod auto-discovery
- [x] Delete for me / Delete for everyone — per-user message hiding + real-time broadcast
- [x] Inline message editing — textarea on bubble, Enter to save, Escape to cancel

---

## ✅ Sprint 5 — Observability & Polish (Completed) — v5.0.0

- [x] Unit tests — AuthService, MessageService, RoomService (30 tests passing)
- [x] Integration tests — WebSocket flow structure created
- [x] Cursor-based pagination — implemented for message loading
- [x] Admin panel — full UI with user/room management
- [x] Centralized logging — JSON format with Logstash encoder (Loki/Grafana ready)
- [x] Test Coverage — JaCoCo integration for measuring code coverage
- [x] Database Migration — Flyway for version-controlled schema changes
- [x] CI/CD Pipeline — GitHub Actions
- [ ] Code Quality Tools — SonarCloud, Checkstyle (optional, can be added later)
- [ ] Message search with Elasticsearch (deferred - optional for v6.0.0)

---

## ✅ Sprint 6 — Critical Fixes & Code Quality (Completed) — v6.0.0

- [x] Extract magic strings to constants — `WebSocketDestinations` class for all STOMP paths
- [x] Method-level @PreAuthorize — Explicit authorization on all admin endpoints
- [x] User ban/unban feature — Complete implementation with `active` field and role system
- [x] Role-Based Access Control (RBAC) — USER and ADMIN roles properly implemented
- [x] Security hardening — Disabled users cannot login, proper authority mapping

---

## 🚀 Sprint 7 — Future Ideas

- [ ] End-to-end encryption for private messages
- [ ] Push notifications (Firebase FCM)
- [ ] Mobile client (React Native or Flutter)
- [ ] Bot/webhook support — post messages via API key
- [ ] Message search with Elasticsearch (full-text search)
- [ ] Distributed tracing with Sleuth + Zipkin/Jaeger
- [ ] GraphQL API alongside REST
- [ ] Voice/video call support with WebRTC

---

## 📌 Tech Debt

- ~~Add unit tests for `AuthService`, `MessageService`, `RoomService`~~ ✅ Completed
- ~~Add integration tests for WebSocket flow~~ ✅ Created
- ~~Add JaCoCo test coverage reporting~~ ✅ Completed
- ~~Add Flyway database migrations~~ ✅ Completed
- ~~Add structured JSON logging~~ ✅ Completed
- ~~Implement active users tracking in admin stats~~ ✅ Completed
- ~~Extract magic strings (channel prefixes, topic paths) to constants~~ ✅ Completed
- ~~Add `@PreAuthorize` role checks on all admin endpoints~~ ✅ Completed
- ~~Implement user enable/disable feature (ban/unban)~~ ✅ Completed
- [x] Add API rate limiting for WebSocket connections
- [x] Add request/response examples to Swagger documentation (auth + messages)
- [x] Add CI/CD pipeline (GitHub Actions)
- [ ] Add automated security scanning (OWASP) - Optional
- [ ] Increase test coverage to 70%+ (partial — controller tests added)

---

## 📦 Releases

| Version | Date | Description |
|---|---|---|
| v1.0.0 | 2026-04-15 | Initial release — Sprint 1 complete. Core infrastructure: JWT auth, WebSocket, Redis Pub/Sub, PostgreSQL, Docker. |
| v2.0.0 | 2026-05-10 | Sprint 2 complete. Presence system, typing indicator, WebSocket auth interceptor, global error handling, multi-language UI, room deletion, Swagger. |
| v3.0.0 | 2026-05-26 | Sprint 3 complete. Read receipts, message editing, file/image attachments, private rooms with invite, direct messages (DM), unread message count, UI enhancements, critical bug fixes. |
| v4.0.0 | 2026-05-29 | Sprint 4 complete. RabbitMQ STOMP broker relay, Redis rate limiting (Bucket4j), refresh tokens, Kubernetes manifests (Deployment/Service/HPA/Ingress/PVC), Actuator health checks, Prometheus metrics, delete for me/everyone, inline message editing. |
| v5.0.0 | 2026-06-05 | Sprint 5 complete. Test Coverage (JaCoCo), Database migrations (Flyway), Structured logging (JSON/Logstash), Cursor-based pagination, Admin Panel, Unit & Integration tests (30+). |
| v6.0.0 | 2026-06-05 | Sprint 6 hardened. Flyway schema fixes, ban/unban admin UI, WebSocket rate limiting, `@EnableMethodSecurity`, GitHub Actions CI, Swagger examples, performance indexes, `ddl-auto=validate`. |
