package com.pdg.reservation.room.service;

import com.pdg.reservation.room.dto.RoomSearchCondition;
import com.pdg.reservation.room.dto.RoomSearchResponse;
import com.pdg.reservation.room.repository.RoomRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class RoomService {

    private final RoomRepository roomRepository;


    public Page<RoomSearchResponse> getRooms(Long accommodationId,RoomSearchCondition condition, Pageable pageable) {
        return roomRepository.search(accommodationId, condition, pageable);
    }
}
