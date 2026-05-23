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

## 🚧 Sprint 2 — Presence & Polish (In Progress)

- [ ] Presence system test (online/offline/typing)
- [ ] `WebSocketAuthInterceptor` — validate JWT on CONNECT, reject invalid tokens
- [ ] Error handling — global exception handler (`@ControllerAdvice`)
- [ ] Input validation responses — proper 400 responses with field errors
- [ ] Swagger / OpenAPI documentation
- [ ] Fix `open-in-view` warning in JPA config
- [ ] Remove hardcoded dialect from `application.yml`

---

## 📋 Sprint 3 — Advanced Features

- [ ] Typing indicator — broadcast `{username} is typing...` via presence channel
- [ ] Read receipts — track which messages a user has seen
- [ ] Message editing — update content, set `edited: true`
- [ ] File/image attachment support (store URL, serve via S3 or local)
- [ ] Private rooms — invite-only with room access control
- [ ] Direct messages (DM) — 1:1 private chat between users
- [ ] Unread message count per room

---

## 📋 Sprint 4 — Scaling & Production

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

## 🐛 Known Issues

| Issue | Priority | Status |
|---|---|---|
| `open-in-view` JPA warning | Low | Open |
| PostgreSQL dialect explicitly set (unnecessary) | Low | Open |
| No global exception handler | Medium | Planned Sprint 2 |
| WebSocket errors not returned to client | Medium | Planned Sprint 2 |

---

## 📌 Tech Debt

- Add unit tests for `AuthService`, `MessageService`, `RoomService`
- Add integration tests for WebSocket flow
- Extract magic strings (channel prefixes, topic paths) to constants
- Add `@PreAuthorize` role checks on sensitive endpoints
