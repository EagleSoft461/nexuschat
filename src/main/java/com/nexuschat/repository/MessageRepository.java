package com.nexuschat.repository;

import com.nexuschat.model.Message;
import com.nexuschat.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByRoomAndDeletedFalseOrderByCreatedAtDesc(Room room, Pageable pageable);

    long countByRoomAndDeletedFalse(Room room);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.id = :id")
    Optional<Message> findByIdWithSender(@Param("id") Long id);

    // Count messages in a room newer than a given message id (for unread count)
    @Query("SELECT COUNT(m) FROM Message m WHERE m.room = :room AND m.deleted = false AND m.id > :lastReadId")
    long countUnreadMessages(@Param("room") Room room, @Param("lastReadId") Long lastReadId);

    // Find the latest message in a room
    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.room = :room AND m.deleted = false ORDER BY m.createdAt DESC")
    Page<Message> findLatestByRoom(@Param("room") Room room, Pageable pageable);
}
