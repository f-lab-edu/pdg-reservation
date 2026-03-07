package com.pdg.reservation.review.repository;

import com.pdg.reservation.review.dto.ReviewSearchCondition;
import com.pdg.reservation.review.entity.Review;
import com.pdg.reservation.review.enums.ReviewSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.pdg.reservation.accommodation.entity.QAccommodation.accommodation;
import static com.pdg.reservation.payment.entity.QPayment.payment;
import static com.pdg.reservation.reservation.entity.QReservation.reservation;
import static com.pdg.reservation.review.entity.QReview.review;
import static com.pdg.reservation.room.entity.QRoom.room;

@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    public BigDecimal calculateAverageRating(Long accommodationId) {

        Double ratingAvg = jpaQueryFactory
                .select(review.rating.avg())
                .from(review)
                .where(review.accommodation.id.eq(accommodationId))
                .fetchOne();

        // 리뷰가 하나도 없을 경우 null이 반환되므로 BigDecimal.ZERO로 처리
        return (ratingAvg == null) ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP) :
                BigDecimal.valueOf(ratingAvg).setScale(1, RoundingMode.HALF_UP);
    }

    @Override
    public Page<Review> findMyReviews(Long memberId, ReviewSearchCondition condition, Pageable pageable) {
        Integer minRating = condition.getMinRating();

        List<Review> content = jpaQueryFactory
                .selectFrom(review)
                .join(review.accommodation, accommodation).fetchJoin()
                .join(review.reservation, reservation).fetchJoin()
                .leftJoin(reservation.payment, payment).fetchJoin()
                .join(reservation.room, room).fetchJoin()
                .where(
                        review.member.id.eq(memberId),
                        eqMinRating(minRating)
                )
                .orderBy(getOrderSpecifier(condition.getSortType()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, getMyReviewCountQuery(memberId, minRating)::fetchOne);
    }

    @Override
    public Page<Review> findAccommodationReviews(Long accommodationId, ReviewSearchCondition condition, Pageable pageable) {
        Integer minRating = condition.getMinRating();

        List<Review> content = jpaQueryFactory
                .selectFrom(review)
                .join(review.accommodation, accommodation).fetchJoin()
                .join(review.reservation, reservation).fetchJoin()
                .leftJoin(reservation.payment, payment).fetchJoin()
                .join(reservation.room, room).fetchJoin()
                .where(
                        review.accommodation.id.eq(accommodationId),
                        eqMinRating(condition.getMinRating())
                )
                .orderBy(getOrderSpecifier(condition.getSortType()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable, getAccReviewCountQuery(accommodationId, minRating)::fetchOne);
    }

    private JPAQuery<Long> getMyReviewCountQuery(Long memberId, Integer minRating) {
        return jpaQueryFactory.select(review.count())
                .from(review)
                .where(
                        review.member.id.eq(memberId),
                        eqMinRating(minRating)
                );
    }


    private JPAQuery<Long> getAccReviewCountQuery(Long accommodationId, Integer minRating) {
        return jpaQueryFactory.select(review.count())
                .from(review)
                .where(
                        review.accommodation.id.eq(accommodationId),
                        eqMinRating(minRating)
                );
    }








    private BooleanExpression eqMinRating(Integer minRating) {
        return minRating != null ? review.rating.goe(minRating) : null;
    }

    private OrderSpecifier<?> getOrderSpecifier(ReviewSortType sortType) {
        if (sortType == null) return review.createdAt.desc();

        return switch (sortType) {
            case HIGH -> review.rating.desc();
            case LOW -> review.rating.asc();
            case LATEST -> review.createdAt.desc();
        };
    }


}
