package com.pdg.reservation.payment.service;

import com.pdg.kafka.event.PaymentKafkaEvent;
import com.pdg.reservation.payment.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

// 결제 이벤트를 Kafka로 보낸다. DB를 쓰지 않는다
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendConfirmed(PaymentConfirmedEvent event) {
        PaymentKafkaEvent message = new PaymentKafkaEvent(
                event.getMemberId(),
                event.getReservationId(),
                event.getEventId(),
                event.getEventType().name(),
                event.getCreatedAt()
        );
        try {
            kafkaTemplate.send("payment-topic", message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            handleFailure(event.getEventId(), event.getReservationId(), ex);
                            return;
                        }
                        handleSuccess(event.getEventId(), result);
                    });
        } catch (Exception e) {
            log.error("[Kafka Producer] 큐 진입 실패 - eventId: {}, error: {}", event.getEventId(), e.getMessage(), e);
            // errorAlertService.sendMessengerAlert(...);
        }
    }

    private void handleSuccess(String eventId, SendResult<String, Object> result) {
        log.info("[Kafka] 발행 성공 - eventId: {}, offset: {}, partition: {}",
                eventId,
                result.getRecordMetadata().offset(),
                result.getRecordMetadata().partition()
        );
    }

    private void handleFailure(String eventId, Long key, Throwable ex) {
        log.error("[Kafka] 발행 실패 - eventId: {}, key: {}, error: {}",
                eventId, key, ex.getMessage(), ex);
        //errorAlertService.sendMessengerAlert(...);
        // 추후 카프카 발행 실패 시 Outbox 패턴 or DLQ 이관 예정
    }
}
