<div align="center">

# 💬 NexusChat

**Enterprise-grade real-time messaging platform built with Spring Boot, WebSocket, Redis Pub/Sub, and PostgreSQL.**

[![CI](https://github.com/EagleSoft461/nexuschat/actions/workflows/ci.yml/badge.svg)](https://github.com/EagleSoft461/nexuschat/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326CE5?style=flat-square&logo=kubernetes)](https://kubernetes.io/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

[![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-35495E?style=flat-square)](https://stomp.github.io/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?style=flat-square&logo=rabbitmq)](https://www.rabbitmq.com/)

[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=flat-square&logo=flyway)](https://flywaydb.org/)
[![Prometheus](https://img.shields.io/badge/Prometheus-Metrics-E6522C?style=flat-square&logo=prometheus)](https://prometheus.io/)
[![Swagger](https://img.shields.io/badge/Swagger-API_Docs-85EA2D?style=flat-square&logo=swagger)](http://localhost:8080/swagger-ui.html)
[![Maintained](https://img.shields.io/badge/Maintained-Yes-green.svg?style=flat-square)](https://github.com/EagleSoft461/nexuschat)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](https://github.com/EagleSoft461/nexuschat/pulls)

</div>

---

## 📌 Overview

NexusChat is a **production-ready** real-time chat backend that demonstrates enterprise messaging patterns. It supports multi-room chat, JWT-based authentication, online presence tracking, and horizontal scaling via Redis Pub/Sub — meaning multiple application instances can serve clients simultaneously without losing message delivery.

### 🎯 Key Highlights

✅ **Production-Ready** — Database migrations (Flyway), structured logging, test coverage  
✅ **Horizontally Scalable** — Redis Pub/Sub enables multi-instance deployment  
✅ **Enterprise Features** — Rate limiting, health checks, metrics  
✅ **Cloud-Native** — Docker & Kubernetes ready with manifests  
✅ **Well-Tested** — JaCoCo test coverage, unit & integration tests  
✅ **Quality Code** — Clean architecture, best practices

---

## 🏗️ Architecture

```
Client (SockJS/STOMP)
        │
        ▼
┌─────────────────────┐
│   Spring Boot App   │
│                     │
│  ┌───────────────┐  │
│  │ WebSocket     │  │
│  │ Controller    │  │
│  └──────┬────────┘  │
│         │           │
│  ┌──────▼────────┐  │
│  │ Message       │  │
│  │ Service       │  │
│  └──────┬────────┘  │
│         │           │
│  ┌──────▼────────┐  │
│  │ Redis         │  │      ┌─────────────────┐
│  │ Publisher     │──┼─────▶│   Redis Pub/Sub  │
│  └───────────────┘  │      └────────┬────────┘
│                     │               │
│  ┌───────────────┐  │      ┌────────▼────────┐
│  │ Redis         │◀─┼──────│  Subscriber     │
│  │ Subscriber    │  │      └─────────────────┘
│  └──────┬────────┘  │
│         │           │
│  ┌──────▼────────┐  │
│  │ STOMP Broker  │  │
│  └──────┬────────┘  │
└─────────┼───────────┘
          │
          ▼
   /topic/room.{id}
   /topic/presence
```

**Message Flow:**
1. Client sends message via WebSocket to `/app/chat.send`
2. `WebSocketController` delegates to `MessageService`
3. Message is persisted to PostgreSQL
4. `RedisMessagePublisher` publishes to `chat:{roomId}` channel
5. `RedisMessageSubscriber` receives and broadcasts to `/topic/room.{roomId}`
6. All subscribed clients receive the message in real-time

This architecture enables **horizontal scaling** — any number of app instances share the same Redis channel, so clients connected to different instances still receive all messages.

---

## ✨ Features

### Core Features
- 🔐 **JWT Authentication** — stateless auth with access & refresh tokens, rotation on use
- 💬 **Real-time Messaging** — STOMP over SockJS with room-based topics
- 📡 **Redis Pub/Sub** — distributed message broadcasting for horizontal scaling
- 🟢 **Presence System** — online/offline tracking with Redis TTL
- 🏠 **Room Management** — create, join, leave public/private rooms + direct messages
- 💾 **Message Persistence** — full history stored in PostgreSQL with cursor-based pagination
- ✏️ **Message Editing** — edit sent messages with edit indicator
- 🗑️ **Smart Delete** — delete for me (hide) vs delete for everyone (broadcast)
- 📖 **Read Receipts** — track which messages users have seen
- ⌨️ **Typing Indicators** — real-time typing notifications
- 📎 **File Attachments** — support for file/image sharing

### Production Features
- 📊 **Test Coverage** — JaCoCo integration for measuring test coverage
- 🔍 **Code Quality** — Clean architecture following Spring Boot best practices
- 🛡️ **Security** — BCrypt password hashing, JWT tokens, rate limiting
- 🚦 **Rate Limiting** — Bucket4j token bucket (Redis-backed), per-user/IP
- 📝 **Structured Logging** — JSON format with Logstash encoder (ELK/Loki ready)
- 📈 **Metrics & Monitoring** — Prometheus metrics via Spring Boot Actuator
- 🏥 **Health Checks** — Kubernetes-ready liveness/readiness probes
- 🗄️ **Database Migration** — Flyway for version-controlled schema changes
- 🐳 **Containerized** — Multi-stage Dockerfile with non-root user
- ☸️ **Kubernetes Ready** — Complete manifests (Deployment, Service, HPA, Ingress, PVC)
- 🔧 **Admin Panel** — Web UI for user & room management
- 🌐 **API Documentation** — Swagger/OpenAPI with interactive UI

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| WebSocket | Spring WebSocket + STOMP + SockJS |
| Security | Spring Security + JWT (jjwt 0.11) + BCrypt |
| Database | PostgreSQL 16 + Spring Data JPA + Hibernate |
| Cache / Pub-Sub | Redis 7 + Spring Data Redis |
| Message Broker | RabbitMQ 3.13 (STOMP relay) |
| Rate Limiting | Bucket4j (Redis-backed token bucket) |
| Build Tool | Maven |
| Database Migration | Flyway |
| Testing | JUnit 5 + Mockito + Spring Boot Test |
| Code Coverage | JaCoCo |
| Logging | Logback + Logstash JSON Encoder |
| Metrics | Micrometer + Prometheus |
| Containerization | Docker + Docker Compose |
| Orchestration | Kubernetes (manifests included) |
| API Docs | Swagger / OpenAPI 3.0 |

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose

### Run with Docker Compose

```bash
git clone https://github.com/EagleSoft461/nexuschat.git
cd nexuschat
docker-compose up --build
```

The application will be available at `http://localhost:8080`.

### Run Locally

Start dependencies:

```bash
docker run -d --name nexuschat-postgres -p 5432:5432 \
  -e POSTGRES_DB=nexuschat \
  -e POSTGRES_USER=nexuschat \
  -e POSTGRES_PASSWORD=nexuschat \
  postgres:16-alpine

docker run -d --name nexuschat-redis -p 6379:6379 redis:7-alpine
```

Run the application:

```bash
mvn spring-boot:run
```

---

## 📡 API Reference

### 📦 Postman Collection

Import the complete API collection to test all endpoints:
👉 [NexusChat.postman_collection.json](./postman/NexusChat.postman_collection.json)

**Quick Start:**
1. Import the collection into Postman
2. Set `base_url` variable to `http://localhost:8080`
3. Run "Login" request - JWT token will be auto-saved
4. All authenticated endpoints will use the saved token automatically

---

### Authentication

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Register new user | ❌ |
| POST | `/api/auth/login` | Login, returns JWT + refresh token | ❌ |
| POST | `/api/auth/refresh` | Rotate refresh token | ❌ |
| POST | `/api/auth/logout` | Revoke refresh tokens | ❌ |

### Rooms

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/rooms` | List public rooms | ✅ |
| POST | `/api/rooms` | Create a room | ✅ |
| GET | `/api/rooms/my` | My rooms | ✅ |
| GET | `/api/rooms/{id}` | Room details | ✅ |
| POST | `/api/rooms/{id}/join` | Join a room | ✅ |
| POST | `/api/rooms/{id}/leave` | Leave a room | ✅ |
| POST | `/api/rooms/dm` | Start or get DM room | ✅ |
| POST | `/api/rooms/{id}/invite` | Invite user (private room) | ✅ |

### Messages

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/messages` | Send message (REST) | ✅ |
| GET | `/api/messages/room/{roomId}` | Paginated history | ✅ |
| GET | `/api/messages/room/{roomId}/cursor` | Cursor-based history | ✅ |
| PATCH | `/api/messages/{id}` | Edit message | ✅ |
| DELETE | `/api/messages/{id}` | Delete for everyone | ✅ |
| DELETE | `/api/messages/{id}/me` | Delete for me | ✅ |
| GET | `/api/messages/room/{roomId}/unread` | Unread count | ✅ |

### Admin

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/admin/stats` | Platform statistics | ADMIN |
| GET | `/api/admin/users` | List users | ADMIN |
| PATCH | `/api/admin/users/{id}/toggle-status` | Ban/unban user | ADMIN |

---

## 🔌 WebSocket Reference

Connect to `/ws` using SockJS + STOMP with `Authorization: Bearer <token>` header.

### Client → Server

| Destination | Payload | Description |
|---|---|---|
| `/app/chat.send` | `{"roomId": 1, "content": "Hello"}` | Send message to room |
| `/app/presence.ping` | `{}` | Refresh online presence |
| `/app/presence.list` | `{}` | Request online users |

### Server → Client

| Topic | Description |
|---|---|
| `/topic/room.{roomId}` | Incoming messages for a room |
| `/topic/presence` | Presence updates |

---

## 📁 Project Structure

```
src/main/java/com/nexuschat/
├── config/
│   ├── WebSocketConfig.java          # STOMP broker + endpoint config
│   ├── SecurityConfig.java           # JWT filter chain
│   ├── RedisConfig.java              # RedisTemplate + listener container
│   └── WebSocketAuthInterceptor.java # JWT auth on CONNECT frame
├── controller/
│   ├── AuthController.java
│   ├── RoomController.java
│   ├── MessageController.java
│   └── WebSocketController.java
├── model/
│   ├── User.java
│   ├── Room.java
│   ├── Message.java
│   └── RoomMember.java
├── dto/
│   ├── request/
│   └── response/
├── repository/
├── service/
│   ├── AuthService.java
│   ├── RoomService.java
│   ├── MessageService.java
│   ├── PresenceService.java
│   └── RedisMessagePublisher.java
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   └── UserDetailsServiceImpl.java
└── redis/
    ├── RedisMessageSubscriber.java
    └── RedisPresenceHandler.java
```

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.
