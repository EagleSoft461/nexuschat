package com.nexuschat.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {

    @Value("${rate-limit.websocket.capacity:60}")
    private int wsCapacity;

    @Value("${rate-limit.websocket.refill-per-minute:60}")
    private int wsRefill;

    @Autowired
    private ProxyManager<String> bucketProxyManager;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SEND) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/app/")) {
            return message;
        }

        String key = resolveKey(accessor);
        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(wsCapacity)
                        .refillGreedy(wsRefill, Duration.ofMinutes(1))
                        .build())
                .build();

        Bucket bucket = bucketProxyManager.builder().build(key, configSupplier);
        if (!bucket.tryConsume(1)) {
            throw new MessagingException("WebSocket rate limit exceeded. Please slow down.");
        }

        return message;
    }

    private String resolveKey(StompHeaderAccessor accessor) {
        Authentication auth = (Authentication) accessor.getUser();
        if (auth != null && auth.isAuthenticated()) {
            return "rl:ws:user:" + auth.getName();
        }
        String sessionId = accessor.getSessionId();
        return "rl:ws:session:" + (sessionId != null ? sessionId : "anonymous");
    }
}
