package com.pdg.reservation.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

// 페이지 단위 조회 결과(응용 계층). Controller가 PageResponse로 바꾼다.
// 리뷰 첫 페이지처럼 Redis에 저장되는 값이라 JSON 역직렬화가 되도록 기본 생성자를 둔다
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <T> PageInfo<T> from(Page<T> page) {
        return new PageInfo<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
