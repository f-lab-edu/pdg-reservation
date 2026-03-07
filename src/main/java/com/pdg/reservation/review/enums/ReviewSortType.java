package com.pdg.reservation.review.enums;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ReviewSortType {
    LATEST("최신순"),
    HIGH("평점 높은순"),
    LOW("평점 낮은순"),
    ;
    private final String description;
}
