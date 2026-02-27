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

    ;

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;

}
