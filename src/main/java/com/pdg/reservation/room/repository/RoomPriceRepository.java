package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.RoomPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPriceRepository extends JpaRepository<RoomPrice, Long>, RoomPriceRepositoryCustom {
}
