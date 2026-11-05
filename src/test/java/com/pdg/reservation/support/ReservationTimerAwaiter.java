package com.pdg.reservation.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// 예약을 만들면 Redis에 결제 대기 타이머 키가 생기고, 결제하면 Kafka 소비자가 비동기로 지운다.
// 이 키는 API로 볼 수 없어서 Redis를 직접 읽는다. 키 형식은 운영 코드(ReservationRedisRepository)의 값과 같아야 한다.
@Component
public class ReservationTimerAwaiter {

    private static final String KEY_PREFIX = "pending_reservation:";
    private static final Duration MAX_WAIT = Duration.ofSeconds(10);

    private final StringRedisTemplate redisTemplate;

    public ReservationTimerAwaiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean timerExists(Long reservationId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + reservationId));
    }

    public void awaitTimerDeleted(Long reservationId) {
        await().atMost(MAX_WAIT).untilAsserted(() -> assertThat(timerExists(reservationId)).isFalse());
    }
}
