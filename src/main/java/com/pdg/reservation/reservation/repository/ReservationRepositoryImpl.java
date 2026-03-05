package com.pdg.reservation.reservation.repository;

import com.pdg.reservation.accommodation.entity.QAccommodationImage;
import com.pdg.reservation.accommodation.enums.ImageType;
import com.pdg.reservation.reservation.dto.QReservationSearchResponse;
import com.pdg.reservation.reservation.dto.ReservationSearchCondition;
import com.pdg.reservation.reservation.dto.ReservationSearchResponse;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

import static com.pdg.reservation.accommodation.entity.QAccommodation.accommodation;
import static com.pdg.reservation.reservation.entity.QReservation.reservation;
import static com.pdg.reservation.review.entity.QReview.review;
import static com.pdg.reservation.room.entity.QRoom.room;

@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<ReservationSearchResponse> search(Long memberId, ReservationSearchCondition condition, Pageable pageable) {

        QAccommodationImage roomImage = new QAccommodationImage("roomImage");
        QAccommodationImage accImage = new QAccommodationImage("accImage");

        List<ReservationSearchResponse> content = jpaQueryFactory
                .select(new QReservationSearchResponse(
                        reservation.id,
                        accommodation.name,
                        room.name,
                        roomImage.url.coalesce(accImage.url), // 객실 메인 우선, 없으면 숙소 메인
                        reservation.checkInDate,
                        reservation.checkOutDate,
                        reservation.guestName,
                        reservation.guestPhoneNumber,
                        reservation.status,
                        reservation.totalPrice,
                        reservation.createdAt,
                        isReviewableExpression(),
                        isCancellableExpression()
                ))
                .from(reservation)
                .leftJoin(reservation.room, room)
                .leftJoin(room.accommodation, accommodation)
                .leftJoin(roomImage).on(
                        roomImage.room.id.eq(room.id),
                        roomImage.imageType.eq(ImageType.ROOM),
                        roomImage.isMain.isTrue()
                )
                .leftJoin(accImage).on(
                        accImage.accommodation.id.eq(accommodation.id),
                        accImage.imageType.eq(ImageType.ACCOMMODATION),
                        accImage.isMain.isTrue()
                )
                .where(
                        reservation.member.id.eq(memberId),
                        statusEq(condition.getStatus()),
                        roomNameLike(condition.getRoomName()),
                        dateBetween(condition.getCheckInDate(), condition.getCheckOutDate())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(reservation.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = getCountQuery(memberId, condition);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private JPAQuery<Long> getCountQuery(Long memberId, ReservationSearchCondition condition) {
        return jpaQueryFactory
                .select(reservation.count())
                .from(reservation)
                .where(
                        reservation.member.id.eq(memberId),
                        statusEq(condition.getStatus())
                );
    }

    /**
     * 취소 가능 여부: CONFIRMED 상태이고 체크인 전일 때
     */
    private BooleanExpression isCancellableExpression() {
        return new CaseBuilder()
                .when(reservation.status.eq(ReservationStatus.CONFIRMED)
                        .and(reservation.checkInDate.after(LocalDate.now())))
                .then(true)
                .otherwise(false);
    }

    /**
     * 리뷰 작성 가능 여부
     * 조건: COMPLETED 상태이며, 해당 예약에 대한 리뷰가 존재하지 않아야 함
     */
    private BooleanExpression isReviewableExpression() {
        return new CaseBuilder()
                .when(reservation.status.eq(ReservationStatus.COMPLETED)
                        .and(JPAExpressions
                                .selectOne()
                                .from(review)
                                .where(review.reservation.id.eq(reservation.id))
                                .exists().not()))
                .then(true)
                .otherwise(false);
    }

    private BooleanExpression statusEq(ReservationStatus status) {
        return status != null ? reservation.status.eq(status) : null;
    }

    private BooleanExpression roomNameLike(String roomName) {
        return StringUtils.hasText(roomName) ? room.name.contains(roomName) : null;
    }

    private BooleanExpression dateBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return null;
        return reservation.checkInDate.goe(start).and(reservation.checkOutDate.loe(end));
    }
}
