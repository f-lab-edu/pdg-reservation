package com.pdg.kafka.consumer;

import com.pdg.kafka.event.ReviewKafkaEvent;
import com.pdg.reservation.accommodation.service.AccommodationRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewKafkaConsumer {

    private final AccommodationRatingService accommodationRatingService;

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".dlt"
    )
    @KafkaListener(
            topics = "review-topic",
            groupId = "rating-group",
            concurrency = "3"
    )
    public void consume(ReviewKafkaEvent event) {
        accommodationRatingService.updateAccommodationRating(event);
    }

    @DltHandler
    public void consumeDlt(ReviewKafkaEvent message,
                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                           @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        log.error("[최종 실패 - DLT], Topic: {}, Message {}, ExceptionMessage: {}", topic, message, exceptionMessage);
        //errorAlertService.sendMessengerAlert(...);
    }


}
