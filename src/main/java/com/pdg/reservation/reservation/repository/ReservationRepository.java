package com.pdg.reservation.reservation.repository;

import com.pdg.reservation.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {

    @EntityGraph(attributePaths = {"payment"})
    //예약 ID와 예약한 유저 ID 일치되는 예약 데이터 조회
    Optional<Reservation> findByIdAndMemberId(Long id, Long memberId);

}
