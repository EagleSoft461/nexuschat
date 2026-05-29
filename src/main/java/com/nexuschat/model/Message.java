package com.nexuschat.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // For IMAGE / FILE messages
    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    // Users who hid this message for themselves (local delete)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "message_hidden_by", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "username", length = 50)
    @Builder.Default
    private java.util.Set<String> hiddenBy = new java.util.HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean edited = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    public enum MessageType {
        TEXT, IMAGE, FILE, SYSTEM
    }
}
