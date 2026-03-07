package com.pdg.reservation.review.dto;

import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.review.entity.Review;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewCreateRequest {

    @NotNull(message = "평점은 선택은 필수입니다.")
    @Digits(integer = 2, fraction = 1) // precision=3, scale=1 대응
    @DecimalMin(value = "0.5", message = "최소 0.5점 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "최대 5.0점까지 가능합니다.")
    private BigDecimal rating;

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(min = 10, max = 300, message = "리뷰는 10자 이상 300자 이하로 작성해주세요.")
    private String content;

    @NotNull(message = "예약 ID는 필수입니다.")
    private Long reservationId;

    public Review toEntity(Member member, Reservation reservation) {
        return Review.builder()
                .member(member)
                .content(this.content)
                .rating(this.rating)
                .nickName(member.getNickName())
                .reservation(reservation)
                .accommodation(reservation.getAccommodation())
                .build();
    }

}
