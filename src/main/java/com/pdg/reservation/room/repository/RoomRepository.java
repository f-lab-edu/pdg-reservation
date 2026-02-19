package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room,Long>, RoomRepositoryCustom {
}
