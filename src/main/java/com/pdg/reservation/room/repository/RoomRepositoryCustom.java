package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.dto.RoomSearchCondition;
import com.pdg.reservation.room.dto.RoomSearchInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RoomRepositoryCustom {
    // 가격은 채우지 않는다. 가격은 RoomPriceService가 계산해서 서비스가 채운다
    Page<RoomSearchInfo> search(Long accommodationId, RoomSearchCondition condition, Pageable pageable);

    // 숙소들 중 기간과 인원 조건에 예약 가능한 객실
    List<Room> findAvailableRooms(Collection<Long> accommodationIds, LocalDate checkInDate, LocalDate checkOutDate, int capacity);
}
