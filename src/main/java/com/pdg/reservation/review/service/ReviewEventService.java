package com.pdg.reservation.review.service;

import com.pdg.kafka.event.ReviewKafkaEvent;
import com.pdg.reservation.review.event.ReviewCreatedEvent;
import com.pdg.reservation.review.event.ReviewDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 리뷰 이벤트를 Kafka로 보낸다. DB를 쓰지 않는다
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendCreated(ReviewCreatedEvent event) {
        send(event.getEventId(), event.getAccommodationId(), event.getReviewId(), event.getRating(),
                event.getEventType().name(), event.getCreatedAt());
    }

    public void sendDeleted(ReviewDeletedEvent event) {
        send(event.getEventId(), event.getAccommodationId(), event.getReviewId(), event.getRating(),
                event.getEventType().name(), event.getCreatedAt());
    }

    private void send(String eventId, Long accommodationId, Long reviewId, BigDecimal rating, String actionType, LocalDateTime createdAt) {
        ReviewKafkaEvent message = new ReviewKafkaEvent(eventId, accommodationId, reviewId, rating, actionType, createdAt);

        try {
            //accommodationId를 키로 지정하여 파티션 순서 보장(기본이 스티키 방식, 키를 넣으면 해시 방식으로 동작)
            //등록 후, 즉시 삭제 처리 시 딜레이 발생하면 삭제가 먼저 시도 될 가능성이 존재하여 동일 파티션으로 보내서 순서 보장)
            kafkaTemplate.send("review-topic", String.valueOf(accommodationId), message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            handleFailure(eventId, reviewId, ex);
                            return;
                        }
                        handleSuccess(eventId, result);
                    });
        } catch (Exception e) {
            log.error("[Kafka Producer] 큐 진입 실패 - eventId: {}, error: {}", eventId, e.getMessage(), e);
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
