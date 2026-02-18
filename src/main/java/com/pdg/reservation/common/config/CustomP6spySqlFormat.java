package com.pdg.reservation.common.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;

import java.util.Locale;

public class CustomP6spySqlFormat implements MessageFormattingStrategy {
    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if (sql == null || sql.trim().isEmpty()) return "";

        if (Category.STATEMENT.getName().equals(category)) {
            String tmpsql = sql.trim().toLowerCase(Locale.ROOT);
            if (tmpsql.startsWith("create") || tmpsql.startsWith("alter") || tmpsql.startsWith("comment")) {
                sql = FormatStyle.DDL.getFormatter().format(sql);
            } else {
                sql = FormatStyle.BASIC.getFormatter().format(sql);
            }
            sql = simplifyDateTime(sql);
            // [P6Spy] 문구를 추가해서 적용 여부를 한눈에 확인하게 함
            return String.format("\n[P6Spy SQL Log] | Execution Time: %d ms%s", elapsed, sql);
        }
        return ""; // commit, rollback 등은 무시
    }

    private String simplifyDateTime(String sql) {
        if (sql == null) return null;

        // 1. T가 대문자인 점 반영
        // 2. 뒤에 붙는 밀리초와 타임존(+0900)까지 확실히 매칭
        // 3. 홑따옴표(') 안에 있는 날짜 포맷 전체를 날짜만 남김
        return sql.replaceAll("(\\d{4}-\\d{2}-\\d{2})T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{4}", "$1");
    }
}