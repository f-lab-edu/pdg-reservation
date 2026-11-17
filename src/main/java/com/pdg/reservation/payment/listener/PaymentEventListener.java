package com.pdg.reservation.payment.listener;

import com.pdg.reservation.payment.event.PaymentConfirmedEvent;
import com.pdg.reservation.payment.facade.PaymentFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentFacade paymentFacade;

    // 외부 시스템(Kafka) 호출이라 커밋 뒤 별도 스레드에서 보낸다. 응답은 이 처리를 기다리지 않는다
    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentConfirmEvent(PaymentConfirmedEvent event) {
        try {
            paymentFacade.publishConfirmed(event);
        } catch (Exception e) {
            // @Async는 호출한 쪽이 예외를 받지 못하므로 여기서 남긴다
            log.error("[Kafka Producer] 결제 확정 이벤트 처리 실패 - eventId: {}", event.getEventId(), e);
        }
    }
}
