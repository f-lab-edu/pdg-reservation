package com.pdg.reservation.review.repository;

import com.pdg.reservation.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

    boolean existsByReservationId(Long reservationId);

}
