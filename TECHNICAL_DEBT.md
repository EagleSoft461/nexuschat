# Technical Debt — NexusChat v6.0.0

Status as of **v6.0.0** release.

## Resolved in v6.0.0

- [x] WebSocket destination constants (`WebSocketDestinations`)
- [x] Method-level `@PreAuthorize` on admin endpoints + `@EnableMethodSecurity`
- [x] User ban/unban (`active` field, service, admin UI, `UserResponse`)
- [x] Flyway migrations aligned with JPA (`active`, `avatar_url`, `type`, `message_hidden_by`)
- [x] `hibernate.ddl-auto=validate` (schema owned by Flyway)
- [x] WebSocket rate limiting (Bucket4j via `WebSocketRateLimitInterceptor`)
- [x] Swagger examples on auth and message endpoints
- [x] Controller and admin service tests
- [x] GitHub Actions CI pipeline
- [x] Performance indexes (V4 migration)
- [x] Removed stray `taskmanager` sample project from repo

## Remaining (optional / future)

- [ ] Real file upload (S3/MinIO) — currently client sends `fileUrl`
- [ ] Test coverage 70%+ (add more controller/repository tests)
- [ ] SonarCloud / Checkstyle integration
- [ ] OWASP dependency scanning in CI
- [ ] Elasticsearch full-text message search
- [ ] Distributed tracing (Micrometer Tracing + Zipkin/Jaeger)
- [ ] End-to-end encryption, push notifications, mobile client (Sprint 7)
