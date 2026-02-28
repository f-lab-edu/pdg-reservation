package com.pdg.reservation.payment.service;


import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.dto.PaymentCancelRequest;
import com.pdg.reservation.payment.dto.PaymentCancelResponse;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.dto.PaymentResponse;
import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.entity.PaymentCancelHistory;
import com.pdg.reservation.payment.repository.PaymentCancelHistoryRepository;
import com.pdg.reservation.payment.repository.PaymentRepository;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.room.entity.RoomInventory;
import com.pdg.reservation.room.repository.RoomInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCommandService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentCancelHistoryRepository paymentCancelHistoryRepository;
    private final RoomInventoryRepository roomInventoryRepository;

    @Transactional
    public PaymentResponse pay(Long reservationId, String pgTransactionNumber, PaymentRequest paymentRequest) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVE_NOT_FOUND));

        Payment payment = paymentRequest.toEntity(reservation, pgTransactionNumber);
        paymentRepository.save(payment);
        reservation.confirmed();
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentCancelResponse cancel(Long reservationId, PaymentCancelRequest cancelRequest) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVE_NOT_FOUND));

        Payment payment = reservation.getPayment();

        List<RoomInventory> inventories = roomInventoryRepository.findAllByRoomAndDateRange(
                reservation.getRoom().getId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate()
        );

        reservation.canceled();
        payment.canceled();
        inventories.forEach(inventory -> {inventory.updateStockStatus(true);});


        PaymentCancelHistory cancelHistory = PaymentCancelHistory.builder()
                .cancelReason(cancelRequest.getCancelReason())
                .cancelAmount(payment.getAmount())
                .payment(payment)
                .build();

        paymentCancelHistoryRepository.save(cancelHistory);
        return PaymentCancelResponse.from(payment);
    }
}
