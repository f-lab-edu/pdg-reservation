package com.pdg.reservation.reservation.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

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
    }

    /**
     * 예약 결제 완료 시 타이머 삭제
     */
    public void deletePaymentTimeout(Long reservationId) {
        String key = KEY_PREFIX + reservationId;
        redisTemplate.delete(key);
    }
}