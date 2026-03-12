package com.pdg.reservation.review.controller;

import com.pdg.reservation.common.auth.security.CustomUserDetails;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.review.dto.*;
import com.pdg.reservation.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewCreateResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewCreateRequest reviewCreateRequest
    ) {
        return ApiResponse.created(reviewService.createReview(userDetails.getMember(), reviewCreateRequest));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> myReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid ReviewSearchCondition condition,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<ReviewResponse> page = reviewService.findMyReviews(userDetails.getMember().getId(), condition, pageable);
        return ApiResponse.ok(PageResponse.from(page));
    }

    @GetMapping("/accommodations/{accommodationId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAccommodationReviews(
            @PathVariable Long accommodationId,
            @Valid ReviewSearchCondition condition,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return ApiResponse.ok(reviewService.getAccommodationReviews(accommodationId, condition, pageable));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewDeleteResponse>> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        return ApiResponse.ok(reviewService.deleteReview(userDetails.getMember().getId(), reviewId));
    }


}
