package com.pdg.reservation.accommodation.repository;

import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.AccommodationSearchResponse;
import com.pdg.reservation.accommodation.entity.Accommodation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AccommodationRepositoryCustom {

    public Page<AccommodationSearchResponse> search(AccommodationSearchCondition condition, Pageable pageable);

}
