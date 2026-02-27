package com.pdg.reservation.reservation.listener;

import com.pdg.reservation.reservation.event.ReservationRollbackEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ReservationEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationRollbackEvent(ReservationRollbackEvent event) {
        log.info("예약 롤백 성공: ReservationId={}", event.reservationId);
    }
}
