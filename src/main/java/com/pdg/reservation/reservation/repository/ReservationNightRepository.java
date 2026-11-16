package com.pdg.reservation.reservation.repository;

import com.pdg.reservation.reservation.entity.ReservationNight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ReservationNightRepository extends JpaRepository<ReservationNight, Long> {

    // 투숙 기간(체크인 ~ 체크아웃 전날)에 이미 점유된 밤이 있는지
    boolean existsByRoomIdAndStayDateGreaterThanEqualAndStayDateLessThan(
            Long roomId, LocalDate checkInDate, LocalDate checkOutDate);

    // 예약이 점유한 밤을 모두 풀어 준다(취소, 만료)
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("delete from ReservationNight n where n.reservation.id = :reservationId")
    void deleteByReservationId(@Param("reservationId") Long reservationId);
}
