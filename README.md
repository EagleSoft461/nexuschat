<div align="center">

# 💬 NexusChat

**Enterprise-grade real-time messaging platform built with Spring Boot, WebSocket, Redis Pub/Sub, and PostgreSQL.**

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-35495E?style=flat-square)](https://stomp.github.io/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)](https://www.docker.com/)
[![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens)](https://jwt.io/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square&logo=apachemaven)](https://maven.apache.org/)

</div>

---

## 📌 Overview

NexusChat is a production-ready real-time chat backend that demonstrates enterprise messaging patterns. It supports multi-room chat, JWT-based authentication, online presence tracking, and horizontal scaling via Redis Pub/Sub — meaning multiple application instances can serve clients simultaneously without losing message delivery.

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

- 🔐 **JWT Authentication** — stateless auth with token validation on WebSocket handshake
- 💬 **Real-time Messaging** — STOMP over SockJS with room-based topics
- 📡 **Redis Pub/Sub** — distributed message broadcasting for horizontal scaling
- 🟢 **Presence System** — online/offline tracking with Redis TTL
- 🏠 **Room Management** — create, join, leave public/private rooms
- 💾 **Message Persistence** — full history stored in PostgreSQL with pagination
- 🗑️ **Soft Delete** — messages are soft-deleted, history is preserved
- 🐳 **Docker Ready** — single command deployment with Docker Compose
- 🔒 **Security** — non-root Docker user, BCrypt password hashing, CSRF disabled for stateless API

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| WebSocket | Spring WebSocket + STOMP + SockJS |
| Security | Spring Security + JWT (jjwt 0.11) |
| Database | PostgreSQL 16 + Spring Data JPA + Hibernate |
| Cache / Pub-Sub | Redis 7 + Spring Data Redis |
| Build | Maven |
| Containerization | Docker + Docker Compose |
| API Docs | Swagger / OpenAPI |

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

### Authentication

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Register new user | ❌ |
| POST | `/api/auth/login` | Login, returns JWT | ❌ |

### Rooms

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/rooms` | List public rooms | ✅ |
| POST | `/api/rooms` | Create a room | ✅ |
| GET | `/api/rooms/my` | My rooms | ✅ |
| GET | `/api/rooms/{id}` | Room details | ✅ |
| POST | `/api/rooms/{id}/join` | Join a room | ✅ |
| POST | `/api/rooms/{id}/leave` | Leave a room | ✅ |

### Messages

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/messages` | Send message (REST) | ✅ |
| GET | `/api/messages/room/{roomId}` | Paginated history | ✅ |
| DELETE | `/api/messages/{id}` | Soft delete message | ✅ |

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
