package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.dto.QRoomSearchResponse;
import com.pdg.reservation.room.dto.RoomSearchCondition;
import com.pdg.reservation.room.dto.RoomSearchResponse;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.pdg.reservation.accommodation.entity.QAccommodationImage.accommodationImage;
import static com.pdg.reservation.room.entity.QRoom.room;
import static com.pdg.reservation.room.entity.QRoomInventory.roomInventory;

@RequiredArgsConstructor
public class RoomRepositoryImpl implements RoomRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<RoomSearchResponse> search(Long accommodationId, RoomSearchCondition condition, Pageable pageable) {
        NumberExpression<Long> priceExpression = roomInventory.price
                .coalesce(room.basePrice.longValue()).castToNum(Long.class);

        List<RoomSearchResponse> content = jpaQueryFactory
                .select(new QRoomSearchResponse(
                        room.id,
                        room.name,
                        room.description,
                        room.maxCapacity,
                        room.roomLabel,
                        room.bedType,
                        priceExpression.sum().as("totalPrice"), //총액
                        priceExpression.avg().as("avgPrice"),   //평균가
                        accommodationImage.url                       // 메인 이미지
                ))
                .from(room)
                .join(roomInventory).on(room.eq(roomInventory.room))
                .leftJoin(accommodationImage).on(
                        accommodationImage.room.eq(room),
                        accommodationImage.isMain.isTrue()
                )
                .where(
                        room.accommodation.id.eq(accommodationId),
                        room.maxCapacity.goe(condition.getCapacity()),
                        roomInventory.inventoryDate.goe(condition.getCheckInDate()),
                        roomInventory.inventoryDate.lt(condition.getCheckOutDate()),
                        RoomExpressions.isRoomAvailable(condition.getCheckInDate(), condition.getCheckOutDate())
                )
                .groupBy(room.id, accommodationImage.url)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(room.id.desc())
                .fetch();

        JPAQuery<Long> countQuery = getCountQuery(accommodationId, condition);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private JPAQuery<Long> getCountQuery(Long accommodationId, RoomSearchCondition condition) {
        return jpaQueryFactory
                .select(room.count())
                .from(room)
                .where(
                        room.accommodation.id.eq(accommodationId),
                        room.maxCapacity.goe(condition.getCapacity()),
                        RoomExpressions.isRoomAvailable(condition.getCheckInDate(), condition.getCheckOutDate())
                );
    }
}
