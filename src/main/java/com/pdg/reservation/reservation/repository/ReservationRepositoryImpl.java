package com.pdg.reservation.reservation.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;
}
