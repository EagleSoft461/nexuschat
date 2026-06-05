package com.nexuschat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Autowired
    private WebSocketRateLimitInterceptor webSocketRateLimitInterceptor;

    @Value("${rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${rabbitmq.stomp-port:61613}")
    private int rabbitStompPort;

    @Value("${rabbitmq.username:nexuschat}")
    private String rabbitUsername;

    @Value("${rabbitmq.password:nexuschat}")
    private String rabbitPassword;

    @Value("${rabbitmq.enabled:true}")
    private boolean rabbitEnabled;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (rabbitEnabled) {
            // Full-featured STOMP broker relay via RabbitMQ
            // Requires RabbitMQ with rabbitmq_stomp plugin enabled
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(rabbitHost)
                    .setRelayPort(rabbitStompPort)
                    .setClientLogin(rabbitUsername)
                    .setClientPasscode(rabbitPassword)
                    .setSystemLogin(rabbitUsername)
                    .setSystemPasscode(rabbitPassword)
                    .setSystemHeartbeatSendInterval(10000)
                    .setSystemHeartbeatReceiveInterval(10000);
        } else {
            // Fallback to in-memory broker (for local dev without RabbitMQ)
            registry.enableSimpleBroker("/topic", "/queue");
        }

        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor, webSocketRateLimitInterceptor);
    }
}
