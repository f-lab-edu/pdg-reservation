package com.pdg.reservation.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    // 응용 계층의 PageInfo를 API 응답으로 바꾼다. 각 항목은 mapper로 Response로 바꾼다
    public static <I, R> PageResponse<R> from(PageInfo<I> pageInfo, Function<I, R> mapper) {
        return new PageResponse<>(
                pageInfo.getContent().stream().map(mapper).toList(),
                pageInfo.getPage(),
                pageInfo.getSize(),
                pageInfo.getTotalElements(),
                pageInfo.getTotalPages(),
                pageInfo.isLast()
        );
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}