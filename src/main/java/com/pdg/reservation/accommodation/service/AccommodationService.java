package com.pdg.reservation.accommodation.service;

import com.pdg.reservation.accommodation.dto.AccommodationResponse;
import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;

    public Page<AccommodationResponse> search(AccommodationSearchCondition condition, Pageable pageable) {
        return accommodationRepository.search(condition, pageable);
    }

}
