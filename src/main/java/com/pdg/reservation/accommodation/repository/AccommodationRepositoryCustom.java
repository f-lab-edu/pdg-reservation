package com.pdg.reservation.accommodation.repository;

import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.AccommodationSearchResponse;
import com.pdg.reservation.accommodation.entity.Accommodation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccommodationRepositoryCustom {

    Page<AccommodationSearchResponse> search(AccommodationSearchCondition condition, Pageable pageable);
    Long incrementRating(Long accommodationId, BigDecimal rating);
    Long decrementRating(Long accommodationId, BigDecimal rating);


}
