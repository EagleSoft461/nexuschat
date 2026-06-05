package com.nexuschat.repository;

import com.nexuschat.model.Room;
import com.nexuschat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("SELECT DISTINCT r FROM Room r JOIN FETCH r.createdBy LEFT JOIN FETCH r.members WHERE r.type = :type")
    List<Room> findByType(@Param("type") Room.RoomType type);

    List<Room> findByCreatedBy(User user);

    @Query("SELECT DISTINCT r FROM Room r JOIN FETCH r.createdBy LEFT JOIN FETCH r.members WHERE r IN (SELECT rm.room FROM RoomMember rm WHERE rm.user = :user)")
    List<Room> findRoomsByMember(@Param("user") User user);

    @Query("SELECT r FROM Room r JOIN FETCH r.createdBy LEFT JOIN FETCH r.members WHERE r.id = :id")
    Optional<Room> findByIdWithCreator(@Param("id") Long id);

    boolean existsByName(String name);

    // Find existing DM room between two users
    @Query("""
        SELECT DISTINCT r FROM Room r
        JOIN FETCH r.createdBy
        LEFT JOIN FETCH r.members
        WHERE r.type = 'DIRECT'
        AND EXISTS (SELECT rm1 FROM RoomMember rm1 WHERE rm1.room = r AND rm1.user = :user1)
        AND EXISTS (SELECT rm2 FROM RoomMember rm2 WHERE rm2.room = r AND rm2.user = :user2)
        """)
    java.util.Optional<Room> findDirectMessageRoom(
            @Param("user1") com.nexuschat.model.User user1,
            @Param("user2") com.nexuschat.model.User user2);

    // Count rooms by type
    long countByType(Room.RoomType type);
}
