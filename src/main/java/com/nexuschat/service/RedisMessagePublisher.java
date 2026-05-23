package com.nexuschat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisMessagePublisher.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void publishMessage(String channel, Object message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for channel {}: {}", channel, e.getMessage());
        }
    }

    public void publishPresence(String username, String status) {
        String channel = "presence:" + username;
        String payload = String.format("{\"username\":\"%s\",\"status\":\"%s\"}", username, status);
        redisTemplate.convertAndSend(channel, payload);
    }
}
