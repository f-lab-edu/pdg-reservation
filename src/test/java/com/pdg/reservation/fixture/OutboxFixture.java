package com.pdg.reservation.fixture;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// 아웃박스 기록은 API 응답에 나오지 않아서 DB를 직접 조회해 확인한다
@Component
public class OutboxFixture {

    private final JdbcTemplate jdbcTemplate;

    public OutboxFixture(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 상태(PENDING, SENT)별 행 수
    public long countByStatus(String status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE status = ?", Long.class, status);
        return count == null ? 0 : count;
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_event", Long.class);
        return count == null ? 0 : count;
    }

    // 전송을 시도한 최대 횟수(성공한 시도도 센다)
    public int maxAttemptCount() {
        Integer max = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(attempt_count), 0) FROM outbox_event", Integer.class);
        return max == null ? 0 : max;
    }
}
