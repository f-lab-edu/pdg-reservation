package com.pdg.reservation.reservation.repository;

import com.pdg.reservation.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {

    @EntityGraph(attributePaths = {"payment"})
    // 예약의 외부 식별자(public_id)와 예약한 회원 id가 모두 일치하는 예약을 조회한다. 남의 예약이면 결과가 없다
    Optional<Reservation> findByPublicIdAndMemberId(UUID publicId, Long memberId);

}
