package com.pdg.reservation.payment.controller;

import com.pdg.reservation.common.auth.security.CustomUserDetails;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.dto.PaymentResponse;
import com.pdg.reservation.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentRequest paymentRequest) {
        return ApiResponse.created(paymentService.processPayment(userDetails.getMember().getId(), paymentRequest.getReservationId(), paymentRequest));
    }




}
