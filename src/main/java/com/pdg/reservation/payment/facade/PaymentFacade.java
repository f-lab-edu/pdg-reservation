package com.pdg.reservation.payment.facade;

import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.kafka.event.PaymentKafkaEvent;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.common.idempotency.IdempotencyKeyService;
import com.pdg.reservation.common.service.IdempotencyService;
import com.pdg.reservation.payment.dto.PaymentInfo;
import com.pdg.reservation.payment.event.PaymentConfirmedEvent;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.service.PaymentEventService;
import com.pdg.reservation.payment.service.PaymentService;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PaymentFacade {

    private final PaymentService paymentService;
    private final PaymentEventService paymentEventService;
    private final IdempotencyKeyService idempotencyKeyService;
    private final ReservationService reservationService;
    private final IdempotencyService idempotencyService;
    private final TransactionTemplate writeTx;
    private final TransactionTemplate readOnlyTx;

    public PaymentFacade(PaymentService paymentService,
                         PaymentEventService paymentEventService,
                         IdempotencyKeyService idempotencyKeyService,
                         ReservationService reservationService,
                         IdempotencyService idempotencyService,
                         TransactionTemplate writeTransactionTemplate,
                         @Qualifier("readOnlyTransactionTemplate") TransactionTemplate readOnlyTransactionTemplate) {
        this.paymentService = paymentService;
        this.paymentEventService = paymentEventService;
        this.idempotencyKeyService = idempotencyKeyService;
        this.reservationService = reservationService;
        this.idempotencyService = idempotencyService;
        this.writeTx = writeTransactionTemplate;
        this.readOnlyTx = readOnlyTransactionTemplate;
    }

    // 외부 호출(PG)이 있어서 메서드에는 트랜잭션을 두지 않고, DB 구간만 TransactionTemplate으로 묶는다
    @DistributedLock(key = "'payment:reservationId:' + #reservationId")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentInfo pay(Long memberId, UUID reservationId, PaymentRequest request, String idempotencyKey) {
        // 0. 가장 앞에서 멱등성 키를 선점한다. 이미 쓴 키면 PG를 부르기 전에 거절한다(실패해도 키는 남으므로 클라이언트는 새 키로 다시 시도한다)
        paymentService.validateIdempotencyKey(idempotencyKey);
        if (!idempotencyKeyService.claim(idempotencyKey)) {
            throw new CustomException(ErrorCode.PAYMENT_DUPLICATE_REQUEST);
        }

        // 1. 읽기 검증
        Long reservationPk = readOnlyTx.execute(status -> {
            paymentService.validatePayMethodPolicy(request);
            Reservation reservation = reservationService.findByPublicIdAndMemberId(reservationId, memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_INVALID_REQUEST));
            reservationService.validatePayable(reservation, request.getAmount());
            return reservation.getId();
        });

        // 2. PG 승인(트랜잭션 밖)
        String pgTransactionNumber = paymentService.approve(request);

        // 3. DB 변경은 한 트랜잭션. 예약 상태는 확정 시점에 다시 검증된다. 실패하면 보상은 PG 취소 하나다
        try {
            return writeTx.execute(status -> {
                Reservation reservation = reservationService.getById(reservationPk);
                Payment payment = paymentService.pay(memberId, reservation, pgTransactionNumber, request);
                reservationService.updateConfirmStatus(reservation);
                return PaymentInfo.from(payment);
            });
        } catch (Exception e) {
            log.error("DB 영속화 실패로 인한 보상 트랜잭션 실행. 예약ID: {}", reservationId, e);
            paymentService.compensate(pgTransactionNumber);
            throw e;
        }
    }

    // 결제 확정 이벤트를 Kafka로 보낸다. 외부 호출만 있어서 트랜잭션을 두지 않는다
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void publishConfirmed(PaymentConfirmedEvent event) {
        paymentEventService.sendConfirmed(event);
    }

    // 결제 확정 메시지를 받아 처리한다(Kafka 컨슈머가 호출). 이미 처리한 이벤트는 건너뛴다
    @Transactional
    public void handleConfirmed(PaymentKafkaEvent event) {
        if (idempotencyService.isAlreadyProcessed(event.getEventId(), event.getActionType(), event.getReservationId())) {
            return;
        }
        log.info("[Kafka Consumer] 예약 완료 알림 시작 - 예약 번호 ID: {}", event.getReservationId());

        //외부 API
        //kakaoTalkService.send(...);
        reservationService.clearPaymentTimeout(event.getReservationId());
    }
}
