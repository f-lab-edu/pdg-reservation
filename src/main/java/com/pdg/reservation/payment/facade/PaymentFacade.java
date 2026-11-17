package com.pdg.reservation.payment.facade;

import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.dto.PaymentInfo;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.entity.Payment;
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
    private final ReservationService reservationService;
    private final TransactionTemplate writeTx;
    private final TransactionTemplate readOnlyTx;

    public PaymentFacade(PaymentService paymentService,
                         ReservationService reservationService,
                         TransactionTemplate writeTransactionTemplate,
                         @Qualifier("readOnlyTransactionTemplate") TransactionTemplate readOnlyTransactionTemplate) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
        this.writeTx = writeTransactionTemplate;
        this.readOnlyTx = readOnlyTransactionTemplate;
    }

    // 외부 호출(PG)이 있어서 메서드에는 트랜잭션을 두지 않고, DB 구간만 TransactionTemplate으로 묶는다
    @DistributedLock(key = "'payment:reservationId:' + #reservationId")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentInfo pay(Long memberId, UUID reservationId, PaymentRequest request) {
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
}
