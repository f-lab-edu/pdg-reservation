package com.pdg.reservation.payment.enums;


import com.pdg.reservation.common.exception.enums.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CancelType {
    USER_REQUEST("사용자가 직접 취소", ErrorCode.PAYMENT_PG_CANCEL_REQUEST_FAILED),
    COMPENSATION("시스템 오류로 인한 자동 보상 취소", ErrorCode.PAYMENT_PG_CANCEL_COMPENSATION_FAILED),
    ;

    private final String description;
    private final ErrorCode errorCode;


}
