package com.pdg.reservation.accommodation.repository;

import com.pdg.reservation.accommodation.dto.AccommodationResponse;
import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.QAccommodationResponse;
import com.pdg.reservation.accommodation.enums.AccommodationType;
import com.pdg.reservation.accommodation.enums.ImageType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.pdg.reservation.accommodation.entity.QAccommodation.*;
import static com.pdg.reservation.accommodation.entity.QAccommodationImage.*;
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

        // * 최저가를 구하기 위한 단계(QueryDsl은 서브쿼리에 limit 지원하지 않아 별도 과정 진행)
        // 1. 검색된 숙소 ID 리스트 추출
        List<Long> accIds = content.stream().map(AccommodationResponse::getId).toList();

        // 2. 최저가 데이터 조회 및 매핑
        if (!accIds.isEmpty()) {
            Map<Long, Long> minPriceMap = getMinPriceMap(accIds, condition);
            content.forEach(dto -> dto.updateMinPrice(minPriceMap.get(dto.getId())));
        }

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
                        roomInventory.isStocked.isTrue(),
                        roomInventory.inventoryDate.goe(condition.getCheckInDate()),    // 체크인부터
                        roomInventory.inventoryDate.lt(condition.getCheckOutDate())     // 체크아웃 전날까지
                )
                .groupBy(room.id) // 방별로 그룹화하여
                .having(roomInventory.count().eq(stayDays)) // 투숙 기간 '모든 날짜'에 재고가 있는지 확인
                .exists(); // 조건을 만족하는 방이 하나라도 있다면 TRUE
    }

    private Map<Long, Long> getMinPriceMap(List<Long> accIds, AccommodationSearchCondition cond) {
        long stayDays = ChronoUnit.DAYS.between(cond.getCheckInDate(), cond.getCheckOutDate());

        // QueryDSL의 FROM 절 서브쿼리 미지원 대응 및 페이징된 ID 대상 조회를 통한 성능 최적화를 위해 2단계 집계 방식을 사용합니다.

        // 1. 객실별 평균가를 계산하는 표현식 (SUM만 사용)
        NumberExpression<Long> avgPricePerRoom = roomInventory.price.coalesce(room.basePrice)
                .sum().divide(stayDays).castToNum(Long.class);

        return jpaQueryFactory
                .select(room.accommodation.id, avgPricePerRoom) // 숙소ID와 객실평균가 조회
                .from(room)
                .join(roomInventory).on(roomInventory.room.eq(room))
                .where(
                        room.accommodation.id.in(accIds),
                        room.maxCapacity.goe(cond.getCapacity()),
                        roomInventory.isStocked.isTrue(),
                        roomInventory.inventoryDate.goe(cond.getCheckInDate()),
                        roomInventory.inventoryDate.lt(cond.getCheckOutDate())
                )
                .groupBy(room.id) // 객실(Room)별로 그룹화하여 1박 평균가 산출
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(room.accommodation.id), // Key: 숙소 ID
                        tuple -> Optional.ofNullable(tuple.get(avgPricePerRoom))
                                        .orElse(0L),// Value: 객실 평균가
                        Math::min // 객실 최소값
                ));
    }

}
