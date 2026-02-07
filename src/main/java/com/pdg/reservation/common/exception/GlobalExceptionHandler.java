package com.pdg.reservation.common.exception;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        log.error("[UNEXPECTED_ERROR] {}", ex.getMessage(), ex);
        return ApiResponse.fail(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("[INVALID_INPUT] {}", ex.getMessage(), ex);
        return ApiResponse.fail(ErrorCode.COMMON_BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("[RUNTIME_ERROR] {}", ex.getMessage(), ex);
        return ApiResponse.fail(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        log.info("[AUTH_LOGIN_FAIL] 인증 실패 {}", ex.getMessage());
        return ApiResponse.fail(ErrorCode.AUTH_UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.info("[AUTH_ACCESS_DENIED] 접근 권한 부족 {}", ex.getMessage());
        return ApiResponse.fail(ErrorCode.AUTH_FORBIDDEN);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.info("[AUTH_USER_NOT_FOUND] 유저 정보 불일치 : {}", ex.getMessage());
        return ApiResponse.fail(ErrorCode.AUTH_USER_NOT_FOUND);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.info("[NOT_FOUND] 해당 경로 없음 : {}", ex.getMessage());
        return ApiResponse.fail(ErrorCode.COMMON_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.info("[NOT_SUPPORTED_METHOD] 해당 메소드 미지원 : {}", ex.getMessage());
        return ApiResponse.fail(ErrorCode.COMMON_METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.info("[MESSAGE_NOT_READABLE] JSON 요청 규격 불일치 : {}", ex.getMessage());
        return ApiResponse.fail(ErrorCode.REQUEST_MESSAGE_NOT_READABLE);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleJwtValidationException(ExpiredJwtException ex) {
        log.info("[EXPIRED_JWT] 만료된 토큰 : {}", ex.getMessage());
        return ApiResponse.fail(ErrorCode.JWT_EXPIRED);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleJwtException(JwtException ex) {
        log.info("[JWT ERROR] JWT 오류 발생 : {}", ex.getMessage());
        return ApiResponse.fail(ErrorCode.JWT_INVALID_TOKEN);

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.info("[INVALID_INPUT] 올바르지 않은 요청 값 : {}", ex.getMessage());
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String detailMessage = fieldError != null ?
                fieldError.getField()  + ", " + fieldError.getDefaultMessage() :
                "입력 값이 올바르지 않습니다.";
        return ApiResponse.fail(ErrorCode.COMMON_INVALID_INPUT, detailMessage);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException ex) {
        log.info("[CUSTOM_ERROR] {} : {}", ex.getErrorCode().name(), ex.getErrorCode().getMessage(), ex);
        return ApiResponse.fail(ex.getErrorCode());
    }



}
