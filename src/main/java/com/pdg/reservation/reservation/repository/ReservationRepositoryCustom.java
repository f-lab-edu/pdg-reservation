package com.pdg.reservation.reservation.repository;

import com.pdg.reservation.reservation.dto.ReservationSearchCondition;
import com.pdg.reservation.reservation.dto.ReservationSearchInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservationRepositoryCustom {

    Page<ReservationSearchInfo> search(Long memberId, ReservationSearchCondition condition, Pageable pageable);
}
