package com.pdg.reservation.reservation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    // 1. 예약 신청 (결제 대기 중) - 10분 내 미입금 시 EXPIRED로 변경됨
    PENDING_PAYMENT("결제 대기"),

    // 2. 예약 확정 (결제 완료) - 정상적인 예약 상태
    CONFIRMED("예약 확정"),

    // 취소 진행 중 - PG 취소를 요청하기 전에 이 상태로 바꾼다. 확정까지 끝나면 CANCELED, PG가 거절하면 CONFIRMED로 돌아간다
    CANCELING("취소 진행 중"),
    // 3. 예약 취소 (사용자/관리자 취소) - 환불 처리 필요
    CANCELED("예약 취소"),

    // 4. 이용 완료 (체크아웃 이후) - 리뷰 작성 가능 상태
    COMPLETED("이용 완료"),

    // 5. 시간 초과 (시스템 취소) - 결제 안 하고 보류 (재고 복구용)
    EXPIRED("시간 초과")
    ;

    private final String description;

}
