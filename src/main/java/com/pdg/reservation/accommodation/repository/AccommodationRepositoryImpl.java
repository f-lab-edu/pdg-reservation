package com.pdg.reservation.accommodation.repository;

import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.AccommodationSearchResponse;
import com.pdg.reservation.accommodation.dto.QAccommodationSearchResponse;
import com.pdg.reservation.accommodation.enums.AccommodationType;
import com.pdg.reservation.accommodation.enums.ImageType;
import com.pdg.reservation.room.repository.RoomExpressions;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static com.pdg.reservation.accommodation.entity.QAccommodation.accommodation;
import static com.pdg.reservation.accommodation.entity.QAccommodationImage.accommodationImage;


@RequiredArgsConstructor
public class AccommodationRepositoryImpl implements AccommodationRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    //숙소 검색
    @Override
    public Page<AccommodationSearchResponse> search(AccommodationSearchCondition condition, Pageable pageable) {
        String city = condition.getCity();
        AccommodationType type = condition.getType();

        List<AccommodationSearchResponse> content = jpaQueryFactory
                .select(new QAccommodationSearchResponse(
                        accommodation.id,
                        accommodation.name,
                        accommodation.type,
                        accommodation.address.city,
                        accommodationImage.url.as("mainImage")
                ))
                .from(accommodation)
                .leftJoin(accommodationImage)
                .on(
                        accommodation.id.eq(accommodationImage.accommodation.id),
                        accommodationImage.imageType.eq(ImageType.ACCOMMODATION),
                        accommodationImage.isMain.isTrue()
                )
                .where(
                        cityEq(city),
                        typeEq(type),
                        RoomExpressions.isAccAvailable(
                                condition.getCheckInDate(),
                                condition.getCheckOutDate(),
                                condition.getCapacity()
                        )
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(accommodation.id.desc())
                .fetch();

        JPAQuery<Long> countQuery = getCountQuery(condition, city, type);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private JPAQuery<Long> getCountQuery(AccommodationSearchCondition condition, String city, AccommodationType type) {
        return jpaQueryFactory
                .select(accommodation.count())
                .from(accommodation)
                .where(
                        cityEq(city),
                        typeEq(type),
                        RoomExpressions.isAccAvailable(
                                condition.getCheckInDate(),
                                condition.getCheckOutDate(),
                                condition.getCapacity()
                        )
                );
    }

    public BooleanExpression cityEq(String city) {
        return StringUtils.hasText(city) ? accommodation.address.city.eq(city): null;
    }

    public BooleanExpression typeEq(AccommodationType type) {
        return type != null ?  accommodation.type.eq(type) : null;
    }

    @Override
    public Long incrementRating(Long accommodationId, BigDecimal rating) {
        return jpaQueryFactory
                .update(accommodation)
                .set(accommodation.totalRatingSum, accommodation.totalRatingSum.add(rating))
                .set(accommodation.reviewCount, accommodation.reviewCount.add(1L))
                // MySQL의 UPDATE는 SET을 왼쪽부터 적용하고 앞에서 바꾼 값을 뒤의 식에서 쓴다. 그래서 평균은 갱신된 합계·개수로 계산한다
                .set(accommodation.ratingAverage,
                        Expressions.numberTemplate(BigDecimal.class,
                                "ROUND({0} / {1}, 1)",
                                accommodation.totalRatingSum, accommodation.reviewCount))
                .where(accommodation.id.eq(accommodationId))
                .execute();
    }

    @Override
    public Long decrementRating(Long accommodationId, BigDecimal rating) {
        return jpaQueryFactory
                .update(accommodation)
                .set(accommodation.totalRatingSum, accommodation.totalRatingSum.subtract(rating))
                .set(accommodation.reviewCount, accommodation.reviewCount.subtract(1L))
                // 갱신된 개수가 0이면(마지막 리뷰를 지운 경우) 평균은 0이다. 평균은 갱신된 합계·개수로 계산한다
                .set(accommodation.ratingAverage,
                        Expressions.numberTemplate(
                                BigDecimal.class,
                                "CASE WHEN {0} > 0 THEN ROUND({1} / {0}, 1) ELSE 0 END",
                                accommodation.reviewCount,
                                accommodation.totalRatingSum
                        ))
                .where(
                        accommodation.id.eq(accommodationId)
                                .and(accommodation.reviewCount.gt(0L)) // [필수] 음수 카운트 방지
                )
                .execute();
    }

}
