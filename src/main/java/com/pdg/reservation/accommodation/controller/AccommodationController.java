package com.pdg.reservation.accommodation.controller;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.dto.AccommodationSearchResponse;
import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.AccommodationSearchResponse;
import com.pdg.reservation.accommodation.service.AccommodationService;
import com.pdg.reservation.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accommodations")
public class AccommodationController {

    private final AccommodationService accommodationService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<Page<AccommodationSearchResponse>>> searchAccommodations(
                @Valid AccommodationSearchCondition condition,
                @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return ApiResponse.ok(accommodationService.search(condition, pageable));
    }

    @GetMapping("/{accommodationId}")
    public ResponseEntity<ApiResponse<AccommodationDetailResponse>> getAccommodationDetail(
            @PathVariable Long accommodationId
    ) {
        return ApiResponse.ok(accommodationService.getAccommodationDetail(accommodationId));
    }



}
