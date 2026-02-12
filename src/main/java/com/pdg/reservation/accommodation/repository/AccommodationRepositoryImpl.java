package com.pdg.reservation.accommodation.repository;

import com.pdg.reservation.accommodation.dto.AccommodationResponse;
import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.QAccommodationResponse;
import com.pdg.reservation.accommodation.enums.AccommodationType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.pdg.reservation.accommodation.entity.QAccommodation.*;
import static com.pdg.reservation.accommodation.entity.QRoom.*;
import static com.pdg.reservation.accommodation.entity.QRoomInventory.*;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationRepositoryImpl implements AccommodationCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<AccommodationResponse> search(AccommodationSearchCondition condition, Pageable pageable) {
        String city = condition.getCity();
        AccommodationType type = condition.getType();

        List<AccommodationResponse> content = jpaQueryFactory
                .select(new QAccommodationResponse(
                        accommodation.id,
                        accommodation.name,
                        accommodation.type,
                        accommodation.address.city
                ))
                .from(accommodation)
                .where(
                        cityEq(city),
                        typeEq(type),
                        isAvailable(condition)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(accommodation.id.desc())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(accommodation.count())
                .from(accommodation)
                .where(
                        cityEq(city),
                        typeEq(type),
                        isAvailable(condition)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    public BooleanExpression cityEq(String city) {
        return StringUtils.hasText(city) ? accommodation.address.city.eq(city): null;
    }

    public BooleanExpression typeEq(AccommodationType type) {
        return type != null ?  accommodation.type.eq(type) : null;
    }

    public BooleanExpression isAvailable(AccommodationSearchCondition condition) {

        // 투숙 박수 계산 (예: 24일~26일 = 2박)
        long stayDays = ChronoUnit.DAYS.between(condition.getCheckInDate(), condition.getCheckOutDate());

        // 가용 객실 존재 여부 체크 서브쿼리
        return JPAExpressions
                .selectOne()
                .from(room)
                .join(roomInventory)
                .on(roomInventory.room.eq(room))
                .where(
                        room.accommodation.eq(accommodation),
                        room.maxCapacity.goe(condition.getCapacity()),
                        roomInventory.isStocked.eq(true),
                        roomInventory.inventoryDate.goe(condition.getCheckInDate()),    // 체크인부터
                        roomInventory.inventoryDate.lt(condition.getCheckOutDate())     // 체크아웃 전날까지
                )
                .groupBy(room.id) // 방별로 그룹화하여
                .having(roomInventory.count().eq(stayDays)) // 투숙 기간 '모든 날짜'에 재고가 있는지 확인
                .exists(); // 조건을 만족하는 방이 하나라도 있다면 TRUE
    }



}
