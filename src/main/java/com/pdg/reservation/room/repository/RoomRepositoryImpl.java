package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.dto.QRoomSearchInfo;
import com.pdg.reservation.room.dto.RoomSearchCondition;
import com.pdg.reservation.room.dto.RoomSearchInfo;
import com.pdg.reservation.room.entity.Room;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static com.pdg.reservation.accommodation.entity.QAccommodationImage.accommodationImage;
import static com.pdg.reservation.room.entity.QRoom.room;

@RequiredArgsConstructor
public class RoomRepositoryImpl implements RoomRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<RoomSearchInfo> search(Long accommodationId, RoomSearchCondition condition, Pageable pageable) {
        List<RoomSearchInfo> content = jpaQueryFactory
                .select(new QRoomSearchInfo(
                        room.id,
                        room.name,
                        room.description,
                        room.maxCapacity,
                        room.roomLabel,
                        room.bedType,
                        Expressions.constant(0L),                    // 총액: 서비스가 계산해서 채운다
                        Expressions.constant(0.0),                   // 평균가: 서비스가 계산해서 채운다
                        accommodationImage.url                       // 메인 이미지
                ))
                .from(room)
                .leftJoin(accommodationImage).on(
                        accommodationImage.room.eq(room),
                        accommodationImage.isMain.isTrue()
                )
                .where(
                        room.accommodation.id.eq(accommodationId),
                        room.maxCapacity.goe(condition.getCapacity()),
                        RoomExpressions.isRoomAvailable(condition.getCheckInDate(), condition.getCheckOutDate())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(room.id.desc())
                .fetch();

        JPAQuery<Long> countQuery = getCountQuery(accommodationId, condition);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<Room> findAvailableRooms(
            Collection<Long> accommodationIds, LocalDate checkInDate, LocalDate checkOutDate, int capacity) {
        return jpaQueryFactory
                .selectFrom(room)
                .where(
                        room.accommodation.id.in(accommodationIds),
                        room.maxCapacity.goe(capacity),
                        RoomExpressions.isRoomAvailable(checkInDate, checkOutDate)
                )
                .fetch();
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
