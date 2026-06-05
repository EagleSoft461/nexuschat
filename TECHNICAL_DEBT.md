# 🔧 NexusChat — Technical Debt & Missing Features

Bu dokümanda projedeki **eksiklikler**, **teknik borçlar** ve **iyileştirme alanları** detaylı şekilde listelenmiştir.

---

## 🚨 **KRİTİK ÖNLEM (High Priority)**

### 1. **Magic String'ler - WebSocket Topics/Channels**
**Sorun:** Kod boyunca sabit string'ler doğrudan kullanılıyor.

**Mevcut Durum:**
```java
// RedisMessageSubscriber.java
messagingTemplate.convertAndSend("/topic/room." + roomId, messageResponse);
messagingTemplate.convertAndSend("/topic/presence", body);

// WebSocketController.java
messagingTemplate.convertAndSend("/topic/room." + roomId + ".read", event);
messagingTemplate.convertAndSend("/topic/room." + roomId + ".typing", event);
```

**Çözüm:** Constants sınıfı oluştur
```java
// src/main/java/com/nexuschat/constant/WebSocketDestinations.java
public final class WebSocketDestinations {
    private WebSocketDestinations() {}
    
    // Topic prefixes
    public static final String TOPIC_PREFIX = "/topic";
    public static final String APP_PREFIX = "/app";
    public static final String QUEUE_PREFIX = "/queue";
    
    // Room topics
    public static final String TOPIC_ROOM = TOPIC_PREFIX + "/room.";
    public static final String TOPIC_ROOM_TYPING = ".typing";
    public static final String TOPIC_ROOM_READ = ".read";
    
    // Presence topics
    public static final String TOPIC_PRESENCE = TOPIC_PREFIX + "/presence";
    
    // User queues
    public static final String QUEUE_PRESENCE_LIST = QUEUE_PREFIX + "/presence.list";
    
    // Helper methods
    public static String getRoomTopic(Long roomId) {
        return TOPIC_ROOM + roomId;
    }
    
    public static String getRoomTypingTopic(Long roomId) {
        return TOPIC_ROOM + roomId + TOPIC_ROOM_TYPING;
    }
    
    public static String getRoomReadTopic(Long roomId) {
        return TOPIC_ROOM + roomId + TOPIC_ROOM_READ;
    }
}
```

**Etki:** 🔴 High (kod maintainability)  
**Efor:** ⏱️ 1-2 saat  
**CV Değeri:** ⭐⭐⭐ (clean code practices)

---

### 2. **Admin Endpoint Security - @PreAuthorize Eksikliği**
**Sorun:** AdminController'da sadece class-level `@PreAuthorize` var, method-level yok.

**Mevcut Durum:**
```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")  // ✅ Class level var
public class AdminController {
    
    @GetMapping("/stats")  // ❌ Method level yok
    public ResponseEntity<AdminStatsResponse> getStats() {
        // ...
    }
}
```

**Sorun Detayı:**
- Class-level `@PreAuthorize` yeterli ANCAK best practice her endpoint'te explicit olması
- Test edilebilirlik daha zor
- Role-based access control (RBAC) daha granular olmalı

**Çözüm:**
```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/stats")
public ResponseEntity<AdminStatsResponse> getStats() { }

@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/users")
public ResponseEntity<List<UserResponse>> getAllUsers() { }

@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{userId}")
public ResponseEntity<Void> deleteUser(@PathVariable Long userId) { }
```

**Etki:** 🟡 Medium (security best practice)  
**Efor:** ⏱️ 30 dakika  
**CV Değeri:** ⭐⭐ (security awareness)

---

### 3. **User Ban/Unban Feature - Incomplete**
**Sorun:** User model'de `enabled` field yok, ban/unban çalışmıyor.

**Mevcut Durum:**
```java
// AdminService.java
public void toggleUserStatus(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    // Alternative: For now, this is a placeholder...
    userRepository.save(user);  // ❌ Hiçbir şey yapmıyor!
}
```

**Çözüm - Adım 1:** User entity'ye field ekle
```java
@Entity
@Table(name = "users")
public class User {
    // ... existing fields
    
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
    
    // getter/setter
}
```

**Çözüm - Adım 2:** Flyway migration
```sql
-- V3__Add_User_Enabled_Field.sql
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true;
CREATE INDEX idx_users_enabled ON users(enabled);
```

**Çözüm - Adım 3:** Service implementation
```java
public void toggleUserStatus(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    user.setEnabled(!user.isEnabled());
    userRepository.save(user);
}
```

**Çözüm - Adım 4:** UserDetailsService'de kontrol
```java
@Override
public UserDetails loadUserByUsername(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    
    if (!user.isEnabled()) {
        throw new DisabledException("User account is disabled");
    }
    
    // ... rest of the code
}
```

**Etki:** 🟡 Medium (functional completeness)  
**Efor:** ⏱️ 2-3 saat  
**CV Değeri:** ⭐⭐⭐ (complete feature implementation)

---

### 4. **WebSocket Rate Limiting - Yok**
**Sorun:** HTTP endpoint'lerde rate limiting var, WebSocket'te yok.

**Mevcut Durum:**
- ✅ RateLimitFilter (HTTP için)
- ❌ WebSocket için rate limiting yok

**Risk:**
- Kullanıcı spam mesaj gönderebilir
- DDoS riski
- Server resource abuse

**Çözüm:** WebSocket Interceptor ile rate limit
```java
@Component
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {
    
    private final Map<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String username = accessor.getUser().getName();
        
        if (accessor.getCommand() == StompCommand.SEND) {
            RateLimiter limiter = userLimiters.computeIfAbsent(username, 
                k -> RateLimiter.create(10.0)); // 10 messages per second
            
            if (!limiter.tryAcquire()) {
                throw new MessagingException("Rate limit exceeded");
            }
        }
        
        return message;
    }
}
```

**Etki:** 🟠 Medium-High (production safety)  
**Efor:** ⏱️ 3-4 saat  
**CV Değeri:** ⭐⭐⭐⭐ (production thinking)

---

## ⚠️ **ORTA ÖNCELİK (Medium Priority)**

### 5. **Swagger Request/Response Examples - Eksik**
**Sorun:** API dokümantasyonunda example yok.

**Mevcut Durum:**
```java
@PostMapping
public ResponseEntity<MessageResponse> sendMessage(
    @Valid @RequestBody SendMessageRequest request) {
    // ...
}
```

**Çözüm:**
```java
@Operation(
    summary = "Send a message to a room",
    description = "Sends a new message to the specified room. All room members will receive it in real-time."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Message sent successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = MessageResponse.class),
            examples = @ExampleObject(
                name = "Success",
                value = """
                {
                  "id": 123,
                  "content": "Hello, World!",
                  "roomId": 1,
                  "senderUsername": "john_doe",
                  "createdAt": "2026-06-05T10:30:00Z",
                  "edited": false
                }
                """
            )
        )
    ),
    @ApiResponse(responseCode = "400", description = "Invalid request"),
    @ApiResponse(responseCode = "403", description = "Not a member of the room")
})
@PostMapping
public ResponseEntity<MessageResponse> sendMessage(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Message details",
        required = true,
        content = @Content(
            examples = @ExampleObject(
                name = "Text Message",
                value = """
                {
                  "roomId": 1,
                  "content": "Hello, World!",
                  "type": "TEXT"
                }
                """
            )
        )
    )
    @Valid @RequestBody SendMessageRequest request,
    @AuthenticationPrincipal UserDetails userDetails) {
    // ...
}
```

**Etki:** 🟡 Medium (documentation quality)  
**Efor:** ⏱️ 4-6 saat (tüm endpoint'ler için)  
**CV Değeri:** ⭐⭐ (attention to detail)

---

### 6. **Test Coverage - Düşük (%50 altında muhtemelen)**
**Sorun:** Sadece 3 servis test edilmiş, controller/repository testleri yok.

**Eksik Testler:**
- ❌ Controller tests (MockMvc)
- ❌ Repository tests (@DataJpaTest)
- ❌ WebSocket integration tests (incomplete)
- ❌ Security tests
- ❌ Redis integration tests

**Hedef:** %70+ coverage

**Çözüm Planı:**

#### **Controller Tests (Öncelikli)**
```java
@WebMvcTest(MessageController.class)
class MessageControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MessageService messageService;
    
    @Test
    @WithMockUser(username = "testuser")
    void sendMessage_shouldReturn200() throws Exception {
        // Arrange
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(1L);
        request.setContent("Hello");
        
        MessageResponse response = new MessageResponse();
        response.setId(1L);
        
        when(messageService.sendMessage(any(), eq("testuser")))
            .thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }
}
```

**Etki:** 🟠 Medium-High (quality assurance)  
**Efor:** ⏱️ 1-2 gün  
**CV Değeri:** ⭐⭐⭐⭐⭐ (shows testing discipline)

---

### 7. **Exception Handling - Standardize Error Responses**
**Sorun:** Error response format tutarsız olabilir.

**Çözüm:** Standard error DTO
```java
@Data
@AllArgsConstructor
public class ErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> fieldErrors; // for validation
}

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse error = new ErrorResponse(
            Instant.now().toString(),
            400,
            "Bad Request",
            "Validation failed",
            request.getRequestURI(),
            fieldErrors
        );
        
        return ResponseEntity.badRequest().body(error);
    }
}
```

**Etki:** 🟡 Medium (API consistency)  
**Efor:** ⏱️ 2-3 saat  
**CV Değeri:** ⭐⭐⭐ (API design)

---

## 💡 **DÜŞÜK ÖNCELİK (Low Priority - İyileştirme)**

### 8. **Database Indexes - Eksik Olabilir**
**Kontrol Edilmesi Gerekenler:**
- Message tablosunda `created_at DESC` index (pagination için)
- Room member tablosunda composite index (`room_id, user_id`)
- Presence için Redis key expiration optimization

**Çözüm:** Flyway migration ile index ekle
```sql
-- V4__Add_Performance_Indexes.sql
CREATE INDEX CONCURRENTLY idx_messages_room_created_at 
ON messages(room_id, created_at DESC) 
WHERE deleted = false;

CREATE INDEX CONCURRENTLY idx_room_members_composite 
ON room_members(room_id, user_id);
```

**Etki:** 🟢 Low-Medium (performance)  
**Efor:** ⏱️ 1 saat  
**CV Değeri:** ⭐⭐⭐ (performance awareness)

---

### 9. **N+1 Query Problem - Kontrol Edilmeli**
**Risk Alanları:**
```java
// MessageService.getRoomMessages() 
// Her message için sender fetch ediliyor mu?

// RoomService.getUserRooms()
// Her room için members fetch ediliyor mu?
```

**Çözüm:** @EntityGraph veya JOIN FETCH kullan
```java
@Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.room.id = :roomId")
Page<Message> findByRoomIdWithSender(@Param("roomId") Long roomId, Pageable pageable);
```

**Kontrol:** Hibernate SQL loglarını aç
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**Etki:** 🟡 Medium (performance)  
**Efor:** ⏱️ 2-4 saat (investigation + fix)  
**CV Değeri:** ⭐⭐⭐⭐ (performance optimization)

---

### 10. **Redis Caching - Sadece Pub/Sub Kullanılıyor**
**Sorun:** Redis sadece message broadcasting için kullanılıyor, caching yok.

**Cache'lenebilecek Veriler:**
- User profile (sık okunan)
- Room details (değişmiyor genelde)
- Public room list

**Çözüm:**
```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#username")
    public UserResponse getUserProfile(String username) {
        // DB'den oku
    }
    
    @CacheEvict(value = "users", key = "#username")
    public void updateUser(String username, UpdateUserRequest request) {
        // Update ve cache'i temizle
    }
}
```

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeValuesWith(
                SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
            );
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

**Etki:** 🟢 Low (performance optimization)  
**Efor:** ⏱️ 3-4 saat  
**CV Değeri:** ⭐⭐⭐⭐ (caching strategies)

---

## 🔮 **GELECEK ÖZELLIKLER (Future - Opsiyonel)**

### 11. **CI/CD Pipeline**
- GitHub Actions
- Automated testing
- Docker image publishing
- SonarCloud code quality

**Etki:** 🟢 Low (şu an gerekli değil)  
**Efor:** ⏱️ 1 gün  
**CV Değeri:** ⭐⭐⭐⭐⭐

---

### 12. **Distributed Tracing**
- Spring Cloud Sleuth
- Zipkin/Jaeger
- Request correlation IDs

**Etki:** 🟢 Low (advanced monitoring)  
**Efor:** ⏱️ 4-6 saat  
**CV Değeri:** ⭐⭐⭐⭐

---

### 13. **Elasticsearch Message Search**
- Full-text search
- Message indexing
- Search API

**Etki:** 🟢 Low (nice-to-have feature)  
**Efor:** ⏱️ 2-3 gün  
**CV Değeri:** ⭐⭐⭐⭐

---

## 📊 **ÖNCELİK MATRISI**

| Feature | Priority | Effort | CV Value | Recommended |
|---------|----------|--------|----------|-------------|
| Magic Strings → Constants | 🔴 High | 1-2h | ⭐⭐⭐ | ✅ YES |
| Method-level @PreAuthorize | 🟡 Medium | 30m | ⭐⭐ | ✅ YES |
| User Ban/Unban | 🟡 Medium | 2-3h | ⭐⭐⭐ | ✅ YES |
| WebSocket Rate Limiting | 🟠 Med-High | 3-4h | ⭐⭐⭐⭐ | ⚠️ MAYBE |
| Swagger Examples | 🟡 Medium | 4-6h | ⭐⭐ | ⚠️ MAYBE |
| Controller Tests | 🟠 Med-High | 1-2d | ⭐⭐⭐⭐⭐ | ⚠️ MAYBE |
| Error Response Format | 🟡 Medium | 2-3h | ⭐⭐⭐ | ⚠️ MAYBE |
| Performance Indexes | 🟢 Low-Med | 1h | ⭐⭐⭐ | ❌ LATER |
| N+1 Query Check | 🟡 Medium | 2-4h | ⭐⭐⭐⭐ | ⚠️ MAYBE |
| Redis Caching | 🟢 Low | 3-4h | ⭐⭐⭐⭐ | ❌ LATER |
| CI/CD Pipeline | 🟢 Low | 1d | ⭐⭐⭐⭐⭐ | ❌ OPTIONAL |
| Distributed Tracing | 🟢 Low | 4-6h | ⭐⭐⭐⭐ | ❌ OPTIONAL |
| Elasticsearch | 🟢 Low | 2-3d | ⭐⭐⭐⭐ | ❌ OPTIONAL |

---

## 🎯 **ÖNERİLER**

### **Şimdi Yap (1-2 gün):**
1. ✅ **Magic Strings → Constants** (1-2 saat) — Quick win, shows clean code
2. ✅ **Method-level @PreAuthorize** (30 dakika) — Security best practice
3. ✅ **User Ban/Unban Complete** (2-3 saat) — Finish incomplete feature

**Toplam:** ~4-6 saat (1 gün rahat)

### **İşe Başvururken Yap (opsiyonel):**
4. ⚠️ **Controller Tests** (1-2 gün) — If you want %70+ coverage
5. ⚠️ **WebSocket Rate Limiting** (3-4 saat) — Shows production thinking

### **İş Bulduktan Sonra Öğren:**
6. ❌ **CI/CD Pipeline** — Gerçek bir projede öğren
7. ❌ **Distributed Tracing** — Advanced topic
8. ❌ **Elasticsearch** — Nice-to-have

---

## 🏆 **SONUÇ**

**Projenin Şu Anki Durumu:** 8/10 ⭐  
**Top 3 Eklerseniz:** 8.5/10 ⭐⭐  
**Tüm Medium Priority'leri Eklerseniz:** 9/10 ⭐⭐⭐  

**Gerçek Tavsiye:** Şu anda projen **YETERLİ**! Top 3'ü yap, interview'a hazırlan. Geri kalanı ileride!
