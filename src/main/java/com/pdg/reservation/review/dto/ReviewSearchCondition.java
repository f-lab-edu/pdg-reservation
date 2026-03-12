package com.pdg.reservation.review.dto;

import com.pdg.reservation.review.enums.ReviewSortType;
import lombok.*;
import org.hibernate.validator.constraints.Range;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewSearchCondition {

    @Range(min = 0, max = 5, message = "평점은 {min}에서 {max} 사이의 값을 입력해주세요.")
    Integer minRating;
    private ReviewSortType sortType = ReviewSortType.LATEST;

    public String toCacheKey(){
        int rating = (minRating == null) ? 0 : minRating;
        return String.format("r:%d:s:%s", rating, sortType);
    }

}

