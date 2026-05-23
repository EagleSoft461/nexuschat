package com.nexuschat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class PresenceService {

    private static final String PRESENCE_KEY_PREFIX = "presence:online:";
    private static final Duration PRESENCE_TTL = Duration.ofMinutes(5);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisMessagePublisher redisMessagePublisher;

    public void setUserOnline(String username) {
        String key = PRESENCE_KEY_PREFIX + username;
        redisTemplate.opsForValue().set(key, "online", PRESENCE_TTL);
        redisMessagePublisher.publishPresence(username, "online");
    }

    public void setUserOffline(String username) {
        String key = PRESENCE_KEY_PREFIX + username;
        redisTemplate.delete(key);
        redisMessagePublisher.publishPresence(username, "offline");
    }

    public boolean isUserOnline(String username) {
        String key = PRESENCE_KEY_PREFIX + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void refreshPresence(String username) {
        String key = PRESENCE_KEY_PREFIX + username;
        redisTemplate.expire(key, PRESENCE_TTL);
    }

    public Set<String> getOnlineUsers() {
        Set<String> keys = redisTemplate.keys(PRESENCE_KEY_PREFIX + "*");
        if (keys == null) return Set.of();
        return keys.stream()
                .map(k -> k.replace(PRESENCE_KEY_PREFIX, ""))
                .collect(java.util.stream.Collectors.toSet());
    }
}
