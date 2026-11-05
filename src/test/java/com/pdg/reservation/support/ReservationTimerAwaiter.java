package com.pdg.reservation.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// 예약을 만들면 Redis에 결제 대기 타이머 키가 생기고, 결제하면 Kafka 소비자가 비동기로 지운다.
// 이 키는 API로 볼 수 없어서 Redis를 직접 읽는다. 키 형식은 운영 코드(ReservationRedisRepository)의 값과 같아야 한다.
// 키에는 예약의 내부 id가 들어가는데 API 응답에는 public_id만 나오므로, public_id로 내부 id를 DB에서 찾는다.
@Component
public class ReservationTimerAwaiter {

    private static final String KEY_PREFIX = "pending_reservation:";
    private static final Duration MAX_WAIT = Duration.ofSeconds(10);

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public ReservationTimerAwaiter(StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean timerExists(UUID reservationPublicId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + internalId(reservationPublicId)));
    }

    public void awaitTimerDeleted(UUID reservationPublicId) {
        Long internalId = internalId(reservationPublicId);
        await().atMost(MAX_WAIT).untilAsserted(
                () -> assertThat(Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + internalId))).isFalse());
    }

    private Long internalId(UUID reservationPublicId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reservation WHERE public_id = UUID_TO_BIN(?)", Long.class, reservationPublicId.toString());
    }
}
