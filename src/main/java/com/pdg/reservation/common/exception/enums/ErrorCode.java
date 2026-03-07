package com.pdg.reservation.common.exception.enums;

import com.pdg.reservation.common.dto.ApiResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-001", "서버 내부 오류가 발생했습니다."),
    COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-002", "요청 값이 올바르지 않습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-003", "요청하신 경로를 찾을 수 없습니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-004", "요청 메서드가 허용되지 않습니다."),
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON-005", "입력 값이 올바르지 않습니다."),

    REQUEST_MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "REQUEST-001", "입력 값 오류 발생, JSON 규격을 확인해주세요."),

    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-001", "인증 정보가 유효하지 않습니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH-002", "접근 권한이 없습니다."),
    AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH-003", "아이디 또는 비밀번호가 일치하지 않습니다."),

    JWT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT-001", "유효하지 않은 토큰입니다."),
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT-002", "유효 기간이 만료된 토큰입니다."),
    JWT_REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED,"JWT-003", "리프레시 토큰이 존재하지 않습니다."),
    JWT_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT-004", "유효하지 않은 리프레시 토큰입니다."),

    ACC_NOT_FOUND(HttpStatus.NOT_FOUND, "ACC-001", "해당 숙소를 찾을 수 없습니다."),

    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM-001", "해당 객실을 찾을 수 없습니다."),

    RESERVE_LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "RESERVE_001", "다른 사용자가 예약을 진행 중입니다. 잠시 후 다시 시도해주세요."),
    RESERVE_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "RESERVE_002", "올바르지 않은 예약 기간입니다. 체크인 및 체크아웃 날짜를 다시 확인해 주세요."),
    RESERVE_ALREADY_BOOKED(HttpStatus.CONFLICT, "RESERVE_003", "선택하신 날짜에 이미 예약이 마감되었습니다. 다른 날짜를 선택해 주세요."),
    RESERVE_PRICE_MISMATCH(HttpStatus.BAD_REQUEST, "RESERVE_004", "결제 요청 금액이 실제 객실 요금과 일치하지 않습니다. 다시 시도해 주세요."),
    RESERVE_EXCEED_MAX_CAPACITY(HttpStatus.BAD_REQUEST,"RESERVE_004", "객실 최대 수용 인원을 초과할 수 없습니다."),
    RESERVE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVE_005", "해당 예약을 찾을 수 없습니다."),
    RESERVE_INVALID_STATUS(HttpStatus.BAD_REQUEST, "RESERVE_006", "현재 예약 상태에서는 요청하신 작업을 수행할 수 없습니다."),

    PAYMENT_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "PAYMENT_001", "유효하지 않은 결제 요청입니다."),
    PAYMENT_MISSING_CARD_COMPANY(HttpStatus.BAD_REQUEST, "PAYMENT_002", "카드 결제 시 카드사 정보는 필수입니다."),
    PAYMENT_INVALID_RESERVE_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_003", "결제 대기 상태만 결제 요청이 가능합니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_004", "결제 가격이 예약 가격과 상이합니다."),
    PAYMENT_PG_CONNECTION_ERROR(HttpStatus.BAD_GATEWAY,"PAYMENT_005","PG사 통신 중 오류가 발생했습니다."),
    PAYMENT_INVALID_CANCEL_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_006", "결제 완료 상태만 취소할 수 있습니다."),

    // 일반 비즈니스 예외 (사용자 재시도 유도)
    PAYMENT_PG_CANCEL_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "PAYMENT_007", "결제 취소 요청 처리 중 오류가 발생했습니다. 다시 시도해 주세요."),
    // 보상 트랜잭션 실패, 시스템 무결성 예외 (내부 관리용 - 매우 중요)
    PAYMENT_PG_CANCEL_COMPENSATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_008", "시스템 오류로 인한 자동 결제 취소에 실패했습니다. 관리자 확인이 필요합니다."),
    PAYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_009", "현재 결제 상태에서는 요청하신 작업을 수행할 수 없습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND,"PAYMENT_010", "연관된 결제 내역을 찾을 수 없습니다."),

    REVIEW_NOT_ELIGIBLE(HttpStatus.BAD_REQUEST, "REVIEW_001", "이용 완료 상태만 리뷰 작성이 가능합니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "REVIEW_002", "이미 작성된 리뷰가 존재합니다."),
    REVIEW_BEFORE_CHECKOUT(HttpStatus.BAD_REQUEST, "REVIEW_003", "리뷰는 체크아웃 날짜 이후부터 작성할 수 있습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_004", "해당 리뷰가 존재하지 않습니다."),
    REVIEW_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "REVIEW_005", "해당 리뷰는 이미 삭제되었습니다."),
    REVIEW_OWNER_MISMATCH(HttpStatus.FORBIDDEN, "REVIEW_006", "본인이 작성한 리뷰만 삭제할 수 있습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;

}
