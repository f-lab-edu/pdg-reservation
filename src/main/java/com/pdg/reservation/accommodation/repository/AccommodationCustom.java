package com.pdg.reservation.accommodation.repository;

import com.pdg.reservation.accommodation.dto.AccommodationResponse;
import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccommodationCustom {

    public Page<AccommodationResponse> search(AccommodationSearchCondition condition, Pageable pageable);
}
