package com.pdg.reservation.accommodation.service;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.AccommodationSearchResponse;
import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;

    public Page<AccommodationSearchResponse> search(AccommodationSearchCondition condition, Pageable pageable) {
        return accommodationRepository.search(condition, pageable);
    }

    public AccommodationDetailResponse getAccommodationDetail(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACC_NOT_FOUND));
        return AccommodationDetailResponse.from(accommodation);
    }

    public void validateExists(Long accommodationId){
        if (!accommodationRepository.existsById(accommodationId)) {
            throw new CustomException(ErrorCode.ACC_NOT_FOUND);
        }
    }
}
