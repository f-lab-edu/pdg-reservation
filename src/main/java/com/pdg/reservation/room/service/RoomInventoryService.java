package com.pdg.reservation.room.service;

import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.room.entity.RoomInventory;
import com.pdg.reservation.room.repository.RoomInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomInventoryService {

    private final RoomInventoryRepository roomInventoryRepository;

    public List<RoomInventory> getStayPeriodInventories(Long roomId, LocalDate checkInDate,  LocalDate checkOutDate) {
        return roomInventoryRepository.findAllByRoomAndDateRange(
                roomId,
                checkInDate,
                checkOutDate
        );
    }

    @Transactional
    public void reduceStock(Long roomId, LocalDate checkInDate,  LocalDate checkOutDate) {
        List<RoomInventory> inventories = getStayPeriodInventories(roomId, checkInDate, checkOutDate);
        inventories.forEach(inventory -> {inventory.updateStockStatus(false);});
    }

    @Transactional
    public void restoreStock(Long roomId, LocalDate checkInDate,  LocalDate checkOutDate) {
        List<RoomInventory> inventories = getStayPeriodInventories(roomId, checkInDate, checkOutDate);
        inventories.forEach(inventory -> {inventory.updateStockStatus(true);});
    }

}
