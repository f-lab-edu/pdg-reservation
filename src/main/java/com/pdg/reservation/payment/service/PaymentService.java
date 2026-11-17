package com.pdg.reservation.payment.service;

import com.pdg.reservation.payment.client.PgClient;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.entity.PaymentCancelHistory;
import com.pdg.reservation.payment.enums.CancelType;
import com.pdg.reservation.payment.event.PaymentConfirmedEvent;
import com.pdg.reservation.payment.repository.PaymentCancelHistoryRepository;
import com.pdg.reservation.payment.repository.PaymentRepository;
import com.pdg.reservation.reservation.entity.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

// 결제 도메인의 흐름. PG 호출 메서드(approve, requestCancel, compensate)에는 트랜잭션을 두지 않는다.
// 호출 순서와 트랜잭션 경계는 PaymentFacade, ReservationFacade가 정한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PgClient pgClient;
    private final PaymentRepository paymentRepository;
    private final PaymentCancelHistoryRepository paymentCancelHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void validatePayMethodPolicy(PaymentRequest request) {
        request.validatePayMethodPolicy();
    }

    // PG 승인. 트랜잭션 밖에서 호출한다
    public String approve(PaymentRequest request) {
        return pgClient.confirm(request);
    }

    // 결제를 저장하고 결제 확정 이벤트를 발행한다. Facade의 쓰기 트랜잭션 안에서 호출한다
    @Transactional
    public Payment pay(Long memberId, Reservation reservation, String pgTransactionNumber, PaymentRequest request) {
        Payment payment = request.toEntity(reservation, pgTransactionNumber);
        paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentConfirmedEvent(
                memberId,
                reservation.getId(),
                UUID.randomUUID().toString(),
                LocalDateTime.now()));
        return payment;
    }

    // PG에 결제 취소를 요청한다. 트랜잭션 밖에서 호출한다
    public void requestCancel(Payment payment) {
        pgClient.cancel(payment.getPgTransactionNumber(), CancelType.USER_REQUEST);
    }

    // 결제를 취소 상태로 바꾸고 취소 이력을 남긴다
    @Transactional
    public void markCanceled(Payment payment, String cancelReason) {
        payment.updateStatusCanceled();
        paymentRepository.save(payment);

        paymentCancelHistoryRepository.save(PaymentCancelHistory.builder()
                .cancelReason(cancelReason)
                .cancelAmount(payment.getAmount())
                .payment(payment)
                .build()
        );
    }

    // PG사 성공 후, DB 예외 시 실행
    public void compensate(String pgTransactionNumber) {
        try {
            pgClient.cancel(pgTransactionNumber, CancelType.COMPENSATION);
        } catch (Exception cancelEx) {
            log.error("보상 취소 실패, 수동 환불 필요. TXNUM_ : {}", pgTransactionNumber, cancelEx);
        }
    }
}
