package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.RoomInventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomInventoryRepository  extends JpaRepository<RoomInventory, Long> , RoomInventoryRepositoryCustom{

}
