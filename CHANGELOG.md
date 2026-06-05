# Changelog

## [6.0.0] - 2026-06-05

### Added
- WebSocket rate limiting (`WebSocketRateLimitInterceptor`)
- GitHub Actions CI (PostgreSQL + Redis services)
- Admin panel ban/unban UI
- Flyway V4 performance indexes
- Swagger request/response examples (auth, messages)
- `AuthControllerTest`, `AdminServiceTest`

### Fixed
- Flyway V3: `active`, `avatar_url`, `message.type`, `message_hidden_by` columns
- `UserResponse` now reflects real `active` status and `role`
- `@EnableMethodSecurity` for admin `@PreAuthorize`
- `application.yml` duplicate `spring:` block merged
- WebSocket integration test CONNECT headers
- Removed stray `taskmanager` sample code from repo
- CI: `chmod +x mvnw` for Linux runners; hardened WebSocket integration tests

### Changed
- `hibernate.ddl-auto` set to `validate` (schema managed by Flyway)
- Integration tests skip when PostgreSQL/Redis are unavailable locally
