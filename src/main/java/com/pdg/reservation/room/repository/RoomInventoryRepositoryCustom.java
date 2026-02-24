package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.RoomInventory;

import java.time.LocalDate;
import java.util.List;

public interface RoomInventoryRepositoryCustom {

    public List<RoomInventory> findAllByRoomAndDateRange(Long roomId, LocalDate checkInDate, LocalDate checkOutDate);
}
