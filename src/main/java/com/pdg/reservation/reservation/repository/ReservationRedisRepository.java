package com.pdg.reservation.reservation.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReservationRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "pending_reservation:";

    /**
     * 예약 결제 대기 타이머 설정
     */
    public void savePaymentTimeout(Long reservationId, long durationMinutes) {
        String key = KEY_PREFIX + reservationId;
        redisTemplate.opsForValue().set(key, "PENDING", durationMinutes, TimeUnit.MINUTES);
        log.info("예약 결제 대기 타이머 시작 , RESERVE ID : {}, DURATION : {}" , reservationId,  durationMinutes);
    }

    /**
     * 예약 결제 완료 시 타이머 삭제
     */
    public void deletePaymentTimeout(Long reservationId) {
        String key = KEY_PREFIX + reservationId;
        try {
            Boolean isDeleted = redisTemplate.delete(key);
            if (isDeleted) {
                log.info("예약 결제 대기 타이머 삭제 성공, RESERVE ID : {}", reservationId);
            } else {
                log.warn("예약 결제 대기 타이머 삭제 무시됨 (키 없음), RESERVE ID : {}", reservationId);
            }
        } catch (Exception e) {
            // 인프라 장애를 비즈니스 실패로 전파하지 않고 격리
            // 스케줄러 롤백 방어 로직에 PENDING 상태가 아니면 롤백하지 않고 리턴하게 되어 있음
            log.error("Redis 통신 오류로 결제 타이머 삭제 실패. RESERVE ID : {} (스케줄러 롤백 방어 로직에 위임함)", reservationId, e);
        }
    }
}