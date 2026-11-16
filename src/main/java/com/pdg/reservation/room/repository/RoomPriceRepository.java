package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.RoomPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RoomPriceRepository extends JpaRepository<RoomPrice, Long> {

    // 기간 [from, to) 안의 객실 하나의 예외 요금. to(체크아웃 날)는 밤이 아니라서 포함하지 않는다
    List<RoomPrice> findByRoomIdAndPriceDateGreaterThanEqualAndPriceDateLessThan(Long roomId, LocalDate from, LocalDate to);

    // 기간 [from, to) 안의 여러 객실의 예외 요금
    List<RoomPrice> findByRoomIdInAndPriceDateGreaterThanEqualAndPriceDateLessThan(Collection<Long> roomIds, LocalDate from, LocalDate to);
}
