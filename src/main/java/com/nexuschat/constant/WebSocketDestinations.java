package com.nexuschat.constant;

/**
 * Constants for WebSocket destination paths.
 * Centralizes all STOMP topic and queue paths to avoid magic strings.
 */
public final class WebSocketDestinations {
    
    private WebSocketDestinations() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    
    // Prefixes
    public static final String TOPIC_PREFIX = "/topic";
    public static final String APP_PREFIX = "/app";
    public static final String QUEUE_PREFIX = "/queue";
    
    // Topic paths
    public static final String TOPIC_ROOM = TOPIC_PREFIX + "/room.";
    public static final String TOPIC_PRESENCE = TOPIC_PREFIX + "/presence";
    
    // Topic suffixes
    public static final String SUFFIX_TYPING = ".typing";
    public static final String SUFFIX_READ = ".read";
    
    // Application destinations
    public static final String APP_CHAT_SEND = APP_PREFIX + "/chat.send";
    public static final String APP_CHAT_EDIT = APP_PREFIX + "/chat.edit";
    public static final String APP_CHAT_DELETE = APP_PREFIX + "/chat.delete";
    public static final String APP_READ_RECEIPT = APP_PREFIX + "/read.receipt";
    public static final String APP_TYPING_INDICATOR = APP_PREFIX + "/typing.indicator";
    public static final String APP_PRESENCE_PING = APP_PREFIX + "/presence.ping";
    public static final String APP_PRESENCE_LIST = APP_PREFIX + "/presence.list";
    
    // User queue
    public static final String QUEUE_PRESENCE_LIST = QUEUE_PREFIX + "/presence.list";
    
    /**
     * Get the topic destination for a specific room
     * @param roomId the room ID
     * @return the topic path (e.g., "/topic/room.123")
     */
    public static String getRoomTopic(Long roomId) {
        return TOPIC_ROOM + roomId;
    }
    
    /**
     * Get the typing indicator topic for a specific room
     * @param roomId the room ID
     * @return the topic path (e.g., "/topic/room.123.typing")
     */
    public static String getRoomTypingTopic(Long roomId) {
        return TOPIC_ROOM + roomId + SUFFIX_TYPING;
    }
    
    /**
     * Get the read receipt topic for a specific room
     * @param roomId the room ID
     * @return the topic path (e.g., "/topic/room.123.read")
     */
    public static String getRoomReadTopic(Long roomId) {
        return TOPIC_ROOM + roomId + SUFFIX_READ;
    }
}
