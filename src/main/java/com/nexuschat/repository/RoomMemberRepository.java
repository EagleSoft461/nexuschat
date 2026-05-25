package com.nexuschat.repository;

import com.nexuschat.model.Room;
import com.nexuschat.model.RoomMember;
import com.nexuschat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    Optional<RoomMember> findByRoomAndUser(Room room, User user);

    List<RoomMember> findByRoom(Room room);

    List<RoomMember> findByUser(User user);

    boolean existsByRoomAndUser(Room room, User user);

    @Transactional
    void deleteByRoomAndUser(Room room, User user);
}
