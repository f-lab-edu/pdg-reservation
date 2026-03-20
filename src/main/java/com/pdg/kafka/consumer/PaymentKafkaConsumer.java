package com.pdg.kafka.consumer;

import com.pdg.kafka.event.PaymentKafkaEvent;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.repository.EventLogRepository;
import com.pdg.reservation.common.service.IdempotencyService;
import com.pdg.reservation.reservation.repository.ReservationRedisRepository;
import com.pdg.reservation.review.service.ReviewCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final ReservationRedisRepository reservationRedisRepository;
    private final IdempotencyService idempotencyService;

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".dlt"
    )
    @KafkaListener(
            topics = "payment-topic",
            groupId = "payment-group",
            concurrency = "3"
    )
    public void consume(PaymentKafkaEvent event) {
        // 멱등성 체크
        if (idempotencyService.isAlreadyProcessed(event.getEventId(), event.getActionType(), event.getReservationId())) {
            return;
        }

        log.info("[Kafka Consumer] 예약 완료 알림 시작 - 예약 번호 ID: {}", event.getReservationId());

        //외부 API
        //kakaoTalkService.send(...);
        reservationRedisRepository.deletePaymentTimeout(event.getReservationId());
    }

    @DltHandler
    public void consumeDlt(PaymentKafkaEvent message,
                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                           @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        log.error("[최종 실패 - DLT], Topic: {}, Message {}, ExceptionMessage: {}", topic, message, exceptionMessage);
        //errorAlertService.sendMessengerAlert(...);
    }


}
