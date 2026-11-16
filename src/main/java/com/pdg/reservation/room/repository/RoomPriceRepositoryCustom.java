package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.RoomPrice;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RoomPriceRepositoryCustom {

    // 투숙 기간 [체크인, 체크아웃)의 객실 하나의 요금 행. 체크아웃 날은 밤이 아니라서 포함하지 않는다
    List<RoomPrice> findInStay(Long roomId, LocalDate checkInDate, LocalDate checkOutDate);

    // 투숙 기간 [체크인, 체크아웃)의 여러 객실의 요금 행
    List<RoomPrice> findInStay(Collection<Long> roomIds, LocalDate checkInDate, LocalDate checkOutDate);
}
