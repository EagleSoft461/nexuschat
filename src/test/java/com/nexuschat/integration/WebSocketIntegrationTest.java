package com.nexuschat.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuschat.dto.request.SendMessageRequest;
import com.nexuschat.dto.response.MessageResponse;
import com.nexuschat.model.Message;
import com.nexuschat.model.Room;
import com.nexuschat.model.RoomMember;
import com.nexuschat.model.User;
import com.nexuschat.repository.RoomMemberRepository;
import com.nexuschat.repository.RoomRepository;
import com.nexuschat.repository.UserRepository;
import com.nexuschat.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIf("com.nexuschat.integration.TestInfrastructure#isAvailable")
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String wsUrl;
    private User testUser;
    private Room testRoom;
    private String jwtToken;

    private long timeoutSeconds() {
        return System.getenv("CI") != null ? 30 : 10;
    }

    @BeforeEach
    void setUp() {
        // SockJS client requires http:// base URL, not ws://
        wsUrl = "http://localhost:" + port + "/ws";

        // Clean up
        roomMemberRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .displayName("Test User")
                .build();
        testUser = userRepository.save(testUser);

        // Create test room
        testRoom = Room.builder()
                .name("Test Room")
                .type(Room.RoomType.PUBLIC)
                .createdBy(testUser)
                .build();
        testRoom = roomRepository.save(testRoom);

        // Add user to room
        RoomMember member = RoomMember.builder()
                .room(testRoom)
                .user(testUser)
                .role(RoomMember.MemberRole.OWNER)
                .build();
        roomMemberRepository.save(member);

        // Generate JWT token
        org.springframework.security.core.userdetails.UserDetails userDetails =
                org.springframework.security.core.userdetails.User.builder()
                        .username(testUser.getUsername())
                        .password(testUser.getPassword())
                        .authorities("ROLE_USER")
                        .build();
        jwtToken = jwtUtil.generateToken(userDetails);
    }

    @Test
    void testWebSocketMessageFlow() throws Exception {
        // Arrange
        BlockingQueue<MessageResponse> receivedMessages = new LinkedBlockingQueue<>();

        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

        MappingJackson2MessageConverter messageConverter =
                new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(messageConverter);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtToken);

        StompSession session = stompClient.connectAsync(
                        wsUrl, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
                            @Override
                            public void handleException(
                                    StompSession session,
                                    StompCommand command,
                                    StompHeaders headers,
                                    byte[] payload,
                                    Throwable exception) {
                                exception.printStackTrace();
                            }

                            @Override
                            public void handleTransportError(
                                    StompSession session,
                                    Throwable exception) {
                                exception.printStackTrace();
                            }
                        }
                )
                .get(timeoutSeconds(), TimeUnit.SECONDS);

        assertNotNull(session);
        assertTrue(session.isConnected());

        // Subscribe to room topic
        session.subscribe("/topic/room." + testRoom.getId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.add((MessageResponse) payload);
            }
        });

        // Give the STOMP broker time to register the subscription
        Thread.sleep(1000);

        // Act - Send message
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(testRoom.getId());
        request.setContent("Hello WebSocket!");
        request.setType(Message.MessageType.TEXT);

        session.send("/app/chat.send", request);

        // Assert - Wait for message
        MessageResponse received = receivedMessages.poll(timeoutSeconds(), TimeUnit.SECONDS);
        assertNotNull(received, "Should receive message via WebSocket");
        assertEquals("Hello WebSocket!", received.getContent());
        assertEquals(testUser.getUsername(), received.getSenderUsername());

        // Cleanup
        session.disconnect();
    }

    @Test
    void testWebSocketAuthenticationFailure() throws Exception {
        // Arrange
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer invalid-token");

        // Act & Assert
        try {
            StompSession session = stompClient.connectAsync(
                            wsUrl, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                    .get(timeoutSeconds(), TimeUnit.SECONDS);
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            fail("Should throw exception for invalid token");
        } catch (Exception e) {
            // Expected - invalid token rejected on STOMP CONNECT
            assertNotNull(e);
        }
    }
}
