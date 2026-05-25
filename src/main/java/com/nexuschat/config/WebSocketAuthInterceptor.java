package com.nexuschat.config;

import com.nexuschat.security.JwtUtil;
import com.nexuschat.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            try {
                String username = jwtUtil.extractUsername(token);
                if (username == null) {
                    throw new org.springframework.security.access.AccessDeniedException("Invalid token");
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!jwtUtil.validateToken(token, userDetails)) {
                    throw new org.springframework.security.access.AccessDeniedException("Token validation failed");
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                accessor.setUser(authentication);

            } catch (org.springframework.security.access.AccessDeniedException e) {
                throw e;
            } catch (Exception e) {
                throw new org.springframework.security.access.AccessDeniedException("Token processing failed: " + e.getMessage());
            }
        }

        return message;
    }
}
