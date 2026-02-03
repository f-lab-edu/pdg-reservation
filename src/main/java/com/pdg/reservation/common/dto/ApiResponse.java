package com.pdg.reservation.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 응답에서 제외
public class ApiResponse<T> {

    private boolean success;
    @Nullable
    private T data;
    @Nullable
    private String message;
    @Nullable
    private String errorCode;

    // ----------------------------------------------------
    // 성공 응답 (200 OK)
    // ----------------------------------------------------

    // 데이터가 없을 때 (예: 삭제 성공)
    public static <T> ResponseEntity<ApiResponse<T>> ok() {
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, null));
    }

    // 데이터가 있을 때 (예: 조회 성공)
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, data, null, null));
    }

    // 생성 성공 (201 Created)
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, data, null, null));
    }

    // ----------------------------------------------------
    // 실패 응답
    // ----------------------------------------------------

    // 메시지와 상태코드만 있는 경우
    public static <T> ResponseEntity<ApiResponse<T>> fail(HttpStatus httpStatus, String message) {
        return ResponseEntity
                .status(httpStatus)
                .body(new ApiResponse<>(false, null, message, null));
    }

    //에러 코드까지 포함하는 경우
    public static <T> ResponseEntity<ApiResponse<T>> fail(HttpStatus httpStatus, String message, String errorCode) {
        return ResponseEntity
                .status(httpStatus)
                .body(new ApiResponse<>(false, null, message, errorCode));
    }

    public static <T> ResponseEntity<ApiResponse<T>> fail(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ApiResponse<>(false, null, errorCode.getMessage(), errorCode.getErrorCode()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> fail(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ApiResponse<>(
                        false,
                        null,
                        message,
                        errorCode.getErrorCode()
                ));
    }
}