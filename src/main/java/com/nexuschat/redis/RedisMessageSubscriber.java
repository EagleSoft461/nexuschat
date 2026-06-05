package com.nexuschat.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuschat.constant.WebSocketDestinations;
import com.nexuschat.dto.response.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMessageSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        try {
            if (channel.startsWith("chat:")) {
                String roomId = channel.substring(5);
                MessageResponse messageResponse = objectMapper.readValue(body, MessageResponse.class);
                // Broadcast to all subscribers of the room topic
                messagingTemplate.convertAndSend(
                    WebSocketDestinations.getRoomTopic(Long.parseLong(roomId)), 
                    messageResponse
                );

            } else if (channel.startsWith("presence:")) {
                // Broadcast presence update to all connected clients
                messagingTemplate.convertAndSend(WebSocketDestinations.TOPIC_PRESENCE, body);
            }
        } catch (Exception e) {
            log.error("Error processing Redis message on channel {}: {}", channel, e.getMessage());
        }
    }
}
