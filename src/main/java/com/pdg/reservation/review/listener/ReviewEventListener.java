package com.pdg.reservation.review.listener;

import com.pdg.reservation.review.event.ReviewCreatedEvent;
import com.pdg.reservation.review.event.ReviewDeletedEvent;
import com.pdg.reservation.review.facade.ReviewFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {

    private final ReviewFacade reviewFacade;

    // 외부 시스템(Kafka) 호출이라 커밋 뒤 별도 스레드에서 보낸다. 응답은 이 처리를 기다리지 않는다
    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        try {
            reviewFacade.publishCreated(event);
        } catch (Exception e) {
            // @Async는 호출한 쪽이 예외를 받지 못하므로 여기서 남긴다
            log.error("[Kafka Producer] 리뷰 등록 이벤트 처리 실패 - eventId: {}", event.getEventId(), e);
        }
    }

    @Async("kafkaEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewDeleted(ReviewDeletedEvent event) {
        try {
            reviewFacade.publishDeleted(event);
        } catch (Exception e) {
            log.error("[Kafka Producer] 리뷰 삭제 이벤트 처리 실패 - eventId: {}", event.getEventId(), e);
        }
    }
}
