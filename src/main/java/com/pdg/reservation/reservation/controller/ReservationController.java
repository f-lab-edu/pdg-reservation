package com.pdg.reservation.reservation.controller;

import com.pdg.reservation.common.auth.security.CustomUserDetails;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.dto.*;
import com.pdg.reservation.reservation.facade.ReservationFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationFacade reservationFacade;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReservationRequest reserveRequest
    ) {
        Member member = userDetails.getMember();
        ReservationInfo info = reservationFacade.reserve(member, reserveRequest);
        return ApiResponse.created(ReservationResponse.from(info));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<ReservationCancelResponse>> cancelPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReservationCancelRequest cancelRequest) {
        ReservationCancelInfo info = reservationFacade.cancel(
                userDetails.getMember().getId(), cancelRequest.getReservationId(), cancelRequest);
        return ApiResponse.ok(ReservationCancelResponse.from(info));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ReservationSearchResponse>>> searchReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid ReservationSearchCondition condition,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<ReservationSearchResponse> page = reservationFacade.search(userDetails.getMember().getId(), condition, pageable);
        return ApiResponse.ok(PageResponse.from(page));
    }
}
