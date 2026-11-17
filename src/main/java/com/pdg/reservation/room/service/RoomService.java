package com.pdg.reservation.room.service;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.room.dto.RoomSearchCondition;
import com.pdg.reservation.room.dto.RoomSearchInfo;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.repository.RoomBlockedDateRepository;
import com.pdg.reservation.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomBlockedDateRepository roomBlockedDateRepository;

    public Page<RoomSearchInfo> search(Long accommodationId, RoomSearchCondition condition, Pageable pageable) {
        return roomRepository.search(accommodationId, condition, pageable);
    }

    public Room getRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    // 투숙 기간(체크인 ~ 체크아웃 전날)에 운영자가 막은 날짜가 있는가
    public boolean isBlocked(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        return roomBlockedDateRepository
                .existsByRoomIdAndBlockedDateGreaterThanEqualAndBlockedDateLessThan(roomId, checkInDate, checkOutDate);
    }
}
