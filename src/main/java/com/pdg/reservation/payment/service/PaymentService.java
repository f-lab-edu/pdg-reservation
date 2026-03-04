package com.pdg.reservation.payment.service;

import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.client.PgClient;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.dto.PaymentResponse;
import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.entity.PaymentCancelHistory;
import com.pdg.reservation.payment.enums.CancelType;
import com.pdg.reservation.payment.repository.PaymentCancelHistoryRepository;
import com.pdg.reservation.payment.repository.PaymentRepository;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.repository.ReservationRedisRepository;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PgClient pgClient;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentCommandService paymentCommandService;
    private final PaymentCancelHistoryRepository paymentCancelHistoryRepository;
    private final ReservationRedisRepository reservationRedisRepository;

    @DistributedLock(key = "'payment:reservationId:' + #reservationId")
    public PaymentResponse processPayment(Long memberId, Long reservationId ,PaymentRequest paymentRequest) {

        paymentRequest.validatePayMethodPolicy();

        Reservation reservation = reservationRepository.findByIdAndMemberId(paymentRequest.getReservationId(), memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_INVALID_REQUEST));

        reservation.validatePayable(paymentRequest.getAmount());

        String pgTransactionNumber = pgClient.confirm(paymentRequest);
        PaymentResponse response = null;

        try {
            response = paymentCommandService.pay(reservationId, pgTransactionNumber, paymentRequest);
        } catch (Exception e) {
            log.error("DB 영속화 실패로 인한 보상 트랜잭션 실행. 예약ID: {}", reservationId, e);
            executeCompensation(pgTransactionNumber);
            throw e;
        }
        reservationRedisRepository.deletePaymentTimeout(reservationId);
        return response;
    }

    @Transactional
    public void cancelPayment(Payment payment, String cancelReason) {

        pgClient.cancel(payment.getPgTransactionNumber(), CancelType.USER_REQUEST);

        payment.updateStatusCanceled();
        paymentRepository.save(payment);

        paymentCancelHistoryRepository.save(PaymentCancelHistory.builder()
                .cancelReason(cancelReason)
                .cancelAmount(payment.getAmount())
                .payment(payment)
                .build()
        );
    }


    //PG사 성공 후, DB 예외 시 실행
    private void executeCompensation(String pgTransactionNumber) {
        try {
            pgClient.cancel(pgTransactionNumber, CancelType.COMPENSATION);
        } catch (Exception cancelEx) {
            log.error("보상 취소 실패, 수동 환불 필요. TXNUM_ : {}", pgTransactionNumber, cancelEx);
        }
    }
}
