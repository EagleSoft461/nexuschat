package com.nexuschat.service;

import com.nexuschat.dto.response.MessageResponse;
import com.nexuschat.model.Message;
import com.nexuschat.search.MessageDocument;
import com.nexuschat.search.MessageSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageSearchService {

    @Autowired
    private MessageSearchRepository searchRepository;

    /**
     * Index a message in Elasticsearch
     */
    public void indexMessage(Message message) {
        MessageDocument document = MessageDocument.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderUsername(message.getSender().getUsername())
                .roomId(message.getRoom().getId())
                .roomName(message.getRoom().getName())
                .createdAt(message.getCreatedAt())
                .type(message.getType().name())
                .deleted(message.isDeleted())
                .build();

        searchRepository.save(document);
    }

    /**
     * Search messages globally
     */
    public List<MessageResponse> searchMessages(String query, int page, int size) {
        Page<MessageDocument> results = searchRepository.findByContentContainingAndDeletedFalse(
                query, PageRequest.of(page, size));

        return results.getContent().stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search messages in a specific room
     */
    public List<MessageResponse> searchMessagesInRoom(Long roomId, String query, int page, int size) {
        Page<MessageDocument> results = searchRepository.findByRoomIdAndContentContainingAndDeletedFalse(
                roomId, query, PageRequest.of(page, size));

        return results.getContent().stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search messages by sender
     */
    public List<MessageResponse> searchMessagesBySender(String username, String query, int page, int size) {
        Page<MessageDocument> results = searchRepository.findBySenderUsernameAndContentContainingAndDeletedFalse(
                username, query, PageRequest.of(page, size));

        return results.getContent().stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete message from index
     */
    public void deleteFromIndex(Long messageId) {
        searchRepository.deleteById(messageId);
    }

    private MessageResponse toMessageResponse(MessageDocument doc) {
        return MessageResponse.builder()
                .id(doc.getId())
                .content(doc.getContent())
                .senderUsername(doc.getSenderUsername())
                .roomId(doc.getRoomId())
                .createdAt(doc.getCreatedAt())
                .type(Message.MessageType.valueOf(doc.getType()))
                .deletedForEveryone(doc.isDeleted())
                .build();
    }
}
