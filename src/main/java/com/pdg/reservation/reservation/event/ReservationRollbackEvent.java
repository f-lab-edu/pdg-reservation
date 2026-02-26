package com.pdg.reservation.reservation.event;

/**
 * 예약 롤백 처리가 완료되었음을 알리는 이벤트 객체
 */
public class ReservationRollbackEvent {

    public Long reservationId;

    public ReservationRollbackEvent(Long reservationId) {
        this.reservationId = reservationId;

    }

}
