package com.pdg.reservation.reservation.controller;

import com.pdg.reservation.common.auth.security.CustomUserDetails;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.dto.ReservationCancelRequest;
import com.pdg.reservation.reservation.dto.ReservationCancelResponse;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.reservation.service.ReservationCancelService;
import com.pdg.reservation.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationCancelService reservationCancelService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ReservationRequest reserveRequest
    ) {
        Member member = userDetails.getMember();
        return ApiResponse.created(reservationService.reserve(reserveRequest.getRoomId(), member, reserveRequest));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<ReservationCancelResponse>> cancelPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReservationCancelRequest cancelRequest) {
        return ApiResponse.ok(reservationCancelService.processCancel(userDetails.getMember().getId(), cancelRequest.getReservationId(), cancelRequest));
    }


}
