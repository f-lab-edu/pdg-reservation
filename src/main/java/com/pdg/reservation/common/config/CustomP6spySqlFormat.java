package com.pdg.reservation.common.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;

import java.util.Locale;

public class CustomP6spySqlFormat implements MessageFormattingStrategy {
    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        // 1. 방어 로직 (Guard Clause): 처리할 필요가 없는 경우 빠르게 종료
        if (sql == null || sql.trim().isEmpty() || !Category.STATEMENT.getName().equals(category)) {
            return "";
        }

        // 2. 포맷 스타일 결정
        FormatStyle style = isDdlStatement(sql) ? FormatStyle.DDL : FormatStyle.BASIC;

        // 3. SQL 포맷팅 및 시간 후처리
        String formattedSql = style.getFormatter().format(sql);
        formattedSql = simplifyDateTime(formattedSql);

        // 4. 최종 로그 반환
        return String.format("\n[P6Spy SQL Log] | Execution Time: %d ms%s", elapsed, formattedSql);
    }

    /**
     * DDL 문(CREATE, ALTER, COMMENT) 여부를 확인합니다.
     */
    private boolean isDdlStatement(String sql) {
        String lowerSql = sql.trim().toLowerCase(Locale.ROOT);
        return lowerSql.startsWith("create")
                || lowerSql.startsWith("alter")
                || lowerSql.startsWith("comment");
    }

    private String simplifyDateTime(String sql) {
        if (sql == null) return null;

        // 1. T가 대문자인 점 반영
        // 2. 뒤에 붙는 밀리초와 타임존(+0900)까지 확실히 매칭
        // 3. 홑따옴표(') 안에 있는 날짜 포맷 전체를 날짜만 남김
        return sql.replaceAll("(\\d{4}-\\d{2}-\\d{2})T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{4}", "$1");
    }
}