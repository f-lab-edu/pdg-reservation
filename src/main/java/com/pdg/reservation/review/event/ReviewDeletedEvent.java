package com.pdg.reservation.review.event;

/**
 * 리뷰 등록 시, 평점 계산을 하기 위한 객체
 */
public class ReviewDeletedEvent {

    public Long accommodationId;

    public ReviewDeletedEvent(Long accommodationId) {
        this.accommodationId = accommodationId;
    }



}
