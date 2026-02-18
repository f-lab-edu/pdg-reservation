package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.dto.RoomSearchCondition;
import com.pdg.reservation.room.dto.RoomSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomRepositoryCustom {
    Page<RoomSearchResponse> search(Long accommodationId, RoomSearchCondition condition, Pageable pageable);
}
