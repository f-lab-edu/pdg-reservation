package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.RoomBlockedDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface RoomBlockedDateRepository extends JpaRepository<RoomBlockedDate, Long> {

    // 투숙 기간(체크인 ~ 체크아웃 전날)에 막힌 날짜가 있는지
    boolean existsByRoomIdAndBlockedDateGreaterThanEqualAndBlockedDateLessThan(
            Long roomId, LocalDate checkInDate, LocalDate checkOutDate);
}
