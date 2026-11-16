package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.RoomSpecialPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomSpecialPriceRepository extends JpaRepository<RoomSpecialPrice, Long>, RoomSpecialPriceRepositoryCustom {
}
