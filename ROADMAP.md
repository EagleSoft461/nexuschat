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

## 📋 Sprint 4 — Scaling & Production (Planned)

- [ ] Replace simple STOMP broker with RabbitMQ (full message broker)
- [ ] Rate limiting — prevent message spam per user
- [ ] Refresh token support — extend session without re-login
- [ ] Pagination improvement — cursor-based instead of offset
- [ ] Kubernetes deployment manifests (Deployment, Service, ConfigMap)
- [ ] Health check endpoint (`/actuator/health`)
- [ ] Metrics with Micrometer + Prometheus
- [ ] Centralized logging with ELK stack or Loki

---

## 💡 Future Ideas

- [ ] End-to-end encryption for private messages
- [ ] Push notifications (Firebase FCM)
- [ ] Mobile client (React Native or Flutter)
- [ ] Message search with Elasticsearch
- [ ] Bot/webhook support — post messages via API key
- [ ] Admin panel — manage users, rooms, messages

---

##  Tech Debt

- Add unit tests for `AuthService`, `MessageService`, `RoomService`
- Add integration tests for WebSocket flow
- Extract magic strings (channel prefixes, topic paths) to constants
- Add `@PreAuthorize` role checks on sensitive endpoints

---

## 📦 Releases

| Version | Date | Description |
|---|---|---|
| v1.0.0 | — | Initial release — Sprint 1 complete. Core infrastructure: JWT auth, WebSocket, Redis Pub/Sub, PostgreSQL, Docker. |
| v2.0.0 | — | Sprint 2 complete. Presence system, typing indicator, WebSocket auth interceptor, global error handling, multi-language UI, room deletion, Swagger. |
| v3.0.0 | 2026-05-26 | Sprint 3 complete. Read receipts, message editing, file/image attachments, private rooms with invite, direct messages (DM), unread message count, UI enhancements, critical bug fixes. |
