package com.pdg.reservation.accommodation.service;

import com.pdg.kafka.event.ReviewKafkaEvent;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.enums.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationRatingService {

    private final AccommodationRepository accommodationRepository;

    // 평점을 갱신한다. 숙소가 없으면 갱신하지 않고 false를 돌려준다
    @Transactional
    public boolean updateRating(ReviewKafkaEvent event) {
        Long updateCount;
        if (EventType.REVIEW_CREATED.name().equals(event.getActionType())) {
            updateCount = accommodationRepository.incrementRating(event.getAccommodationId(), event.getRating());
        } else {
            updateCount = accommodationRepository.decrementRating(event.getAccommodationId(), event.getRating());
        }

        if (updateCount == 0) {
            log.error("평점 변경 실패 - 숙소 없음  (ID: {})", event.getAccommodationId());
            //errorAlertService.sendMessengerAlert(...);
            return false;
        }
        return true;
    }
}
